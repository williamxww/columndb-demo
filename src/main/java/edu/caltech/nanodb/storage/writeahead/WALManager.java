package edu.caltech.nanodb.storage.writeahead;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.caltech.nanodb.transactions.TransactionManager;
import org.apache.log4j.Logger;

import edu.caltech.nanodb.client.SessionState;
import edu.caltech.nanodb.storage.BufferManager;
import edu.caltech.nanodb.storage.DBFile;
import edu.caltech.nanodb.storage.DBFileReader;
import edu.caltech.nanodb.storage.DBFileType;
import edu.caltech.nanodb.storage.DBFileWriter;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.StorageManager;
import edu.caltech.nanodb.transactions.TransactionState;
import edu.caltech.nanodb.util.ArrayUtil;

/**
 * 此类用于管理write-ahead log，一些方法是用于记录各种类型的日志，一些方法则用于根据日志进行恢复。 注意部分日志操作（跟事务相关的）是由
 * {@link TransactionManager}代为完成的， such as syncing the write-ahead log, forcing
 * the WAL, and so forth.
 */
public class WALManager {

    private static Logger logger = Logger.getLogger(WALManager.class);

    public static final String WAL_FILENAME_PATTERN = "wal-%05d.log";

    /**
     * log file number的最大值.
     */
    public static final int MAX_WAL_FILE_NUMBER = 65535;

    /**
     * write-ahead log file 最大 10MB，达到此值时创建新的WAL文件
     */
    public static final int MAX_WAL_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 接下来4byte记录了在前一个WAL file中最后一个字节的偏移量，第一个WAL文件中此值为0
     */
    public static final int OFFSET_PREV_FILE_END = 2;

    /**
     * This is the file-offset of the first log entry in a WAL file.
     */
    public static final int OFFSET_FIRST_RECORD = 6;

    private StorageManager storageManager;

    private BufferManager bufferManager;

    /**
     * 恢复时的起始位置
     */
    private LogSequenceNumber firstLSN;

    /**
     * 下次写日志的位置
     */
    private LogSequenceNumber nextLSN;

    public WALManager(StorageManager storageManager, BufferManager bufferManager) {
        this.storageManager = storageManager;
        this.bufferManager = bufferManager;
    }

    /**
     * 根据文件号创建write ahead log file(WALFile)
     * 
     * @param fileNo 文件号
     * @return DBFile
     * @throws IOException e
     */
    public DBFile createWALFile(int fileNo) throws IOException {
        String filename = getWALFileName(fileNo);
        logger.debug("Creating WAL file " + filename);
        return storageManager.createDBFile(filename, DBFileType.WRITE_AHEAD_LOG_FILE);
    }

    /**
     * 根据文件号，打开指定WAL日志文件
     * 
     * @param fileNo 文件号
     * @return 打开的WAL日志文件
     * @throws IOException 文件不存在或是文件类型不是WAL
     */
    public DBFile openWALFile(int fileNo) throws IOException {
        String filename = getWALFileName(fileNo);
        logger.debug("Opening WAL file " + filename);

        DBFile dbFile = storageManager.openDBFile(filename);
        DBFileType type = dbFile.getType();

        if (type != DBFileType.WRITE_AHEAD_LOG_FILE) {
            throw new IOException(String.format("File %s is not of WAL-file type.", filename));
        }

        return dbFile;
    }

    /**
     * 生成WAL文件名
     * 
     * @param fileNo 文件号
     * @return WAL文件名
     */
    public static String getWALFileName(int fileNo) {
        return String.format(WAL_FILENAME_PATTERN, fileNo);
    }

    public LogSequenceNumber getFirstLSN() {
        return firstLSN;
    }

    public LogSequenceNumber getNextLSN() {
        return nextLSN;
    }

    /**
     * Performs recovery processing starting at the specified log sequence
     * number, and returns the LSN where the next recovery process should start
     * from.
     *
     * @param storedFirstLSN the location of the write-ahead log record where
     *        recovery should start from
     *
     * @param storedNextLSN the location in the write-ahead log that is
     *        <em>just past</em> the last valid log record in the WAL
     *
     * @return the new location where recovery should start from the next time
     *         recovery processing is performed
     * 
     * @throws IOException if an IO error occurs during recovery processing
     */
    public RecoveryInfo doRecovery(LogSequenceNumber storedFirstLSN, LogSequenceNumber storedNextLSN)
            throws IOException {

        firstLSN = storedFirstLSN;
        nextLSN = storedNextLSN;
        RecoveryInfo recoveryInfo = new RecoveryInfo(firstLSN, nextLSN);

        if (firstLSN.equals(nextLSN)) {
            // No recovery necessary! Just return the passed-in info.
            return recoveryInfo;
        }

        performRedo(recoveryInfo);
        performUndo(recoveryInfo);

        TransactionManager txnMgr = storageManager.getTransactionManager();

        // Force the WAL out, up to the nextLSN value. Then, write all dirty
        // data pages, and sync all of the affected files.
        txnMgr.forceWAL(nextLSN);
        bufferManager.writeAll(true);

        // At this point, all files in the database should be in sync with
        // the entirety of the write-ahead log. So, update the firstLSN value
        // and update the transaction state file again. (This won't write out
        // any WAL records, but it will write and sync the txn-state file.)
        firstLSN = nextLSN;
        txnMgr.forceWAL(nextLSN);

        recoveryInfo.firstLSN = firstLSN;
        recoveryInfo.nextLSN = nextLSN;

        return recoveryInfo;
    }

    /**
     * 重做日志
     * 
     * @param recoveryInfo 从起始位置重做日志直到终止位置
     * @throws IOException e
     */
    private void performRedo(RecoveryInfo recoveryInfo) throws IOException {
        LogSequenceNumber currLSN = recoveryInfo.firstLSN;
        logger.debug("Starting redo processing at LSN " + currLSN);

        LogSequenceNumber oldLSN = null;
        DBFileReader walReader = null;
        while (currLSN.compareTo(recoveryInfo.nextLSN) < 0) {
            if (oldLSN == null || oldLSN.getLogFileNo() != currLSN.getLogFileNo()) {
                walReader = getWALFileReader(currLSN);
            }

            // 获取type和transactionID
            byte typeID = walReader.readByte();
            int transactionID = walReader.readInt();
            WALRecordType type = WALRecordType.valueOf(typeID);

            logger.debug(
                    String.format("Redoing WAL record at %s.  Type = %s, TxnID = %d", currLSN, type, transactionID));

            if (type != WALRecordType.START_TXN) {
                // 除了起始事务，其他事务类型都有preLSN (1个LSN：fileNo+offset)
                walReader.movePosition(6);
            }

            // 将此事务transactionID放到recoveryInfo的未完成事务map中
            recoveryInfo.updateInfo(transactionID, currLSN);

            // Redo specific operations.
            switch (type) {
                case START_TXN:
                    // 前面updateInfo已将此事务放到未完成事务map中了，此处不再做处理
                    logger.debug("Transaction " + transactionID + " is starting");
                    // 跳过record末尾的record-type
                    walReader.movePosition(1);
                    break;

                case COMMIT_TXN:
                case ABORT_TXN:
                    logger.debug("Transaction " + transactionID + " is completed (" + type + ")");
                    // 将此事务从未完成map中移除
                    recoveryInfo.recordTxnCompleted(transactionID);
                    // 跳过record末尾的record-type
                    walReader.movePosition(1);

                    break;

                case UPDATE_PAGE:
                case UPDATE_PAGE_REDO_ONLY:
                    // 从WAL中获取要操作的真实数据页
                    String redoFilename = walReader.readVarString255();
                    int redoPageNo = walReader.readUnsignedShort();
                    int numSegments = walReader.readUnsignedShort();

                    // 打开对应的数据页
                    DBFile redoFile = storageManager.openDBFile(redoFilename);
                    DBPage redoPage = storageManager.loadDBPage(redoFile, redoPageNo);

                    logger.debug(String.format("Redoing changes to file %s, page %d (%d segments)", redoFile,
                            redoPageNo, numSegments));

                    // 对数据页执行重做
                    applyRedo(type, walReader, redoPage, numSegments);

                    // 跳过此record末尾的recordSize(int)和recordType(byte)
                    walReader.movePosition(5);
                    break;

                default:
                    throw new WALFileException("Encountered unrecognized WAL record type " + type + " at LSN " + currLSN
                            + " during redo processing!");
            }

            oldLSN = currLSN;
            currLSN = computeNextLSN(currLSN.getLogFileNo(), walReader.getPosition());
        }

        if (currLSN.compareTo(recoveryInfo.nextLSN) != 0) {
            throw new WALFileException(
                    "Traversing WAL file didn't yield " + " the same ending LSN as in the transaction-state file.  WAL "
                            + " result:  " + currLSN + "  TxnState:  " + recoveryInfo.nextLSN);
        }

        logger.debug("Redo processing is complete.  There are " + recoveryInfo.incompleteTxns.size()
                + " incomplete transactions.");
    }

    /**
     * 执行回滚操作，并记录新的日志。将RecoveryInfo中未完成的事务都回退
     * FIXME 这个方法貌似有问题，
     * @param recoveryInfo 指定了要回滚的位置
     * @throws IOException
     */
    private void performUndo(RecoveryInfo recoveryInfo) throws IOException {
        LogSequenceNumber currLSN = recoveryInfo.nextLSN;
        logger.debug("Starting undo processing at LSN " + currLSN);

        LogSequenceNumber oldLSN = null;
        DBFileReader walReader = null;
        while (recoveryInfo.hasIncompleteTxns()) {

            int logFileNo = currLSN.getLogFileNo();
            int fileOffset = currLSN.getFileOffset();

            if (fileOffset < OFFSET_FIRST_RECORD) {
                // 不可能到这里
                throw new WALFileException(
                        String.format("Overshot the start " + "of WAL file %d's records; ended up at file-position %d",
                                logFileNo, fileOffset));
            }

            // fileOffset处于起始位置，则获取前一个文件END的offset
            if (fileOffset == OFFSET_FIRST_RECORD) {
                // 获取前一个WAL文件的offset
                walReader = getWALFileReader(currLSN);
                walReader.setPosition(OFFSET_PREV_FILE_END);
                int prevFileEndOffset = walReader.readInt();
                if (prevFileEndOffset == 0) {
                    logger.debug("Reached the very start of the write-ahead log!");
                    break;
                }

                // 获取前一个WAL文件的fileNo
                logFileNo--;
                if (logFileNo < 0) {
                    logFileNo = MAX_WAL_FILE_NUMBER;
                }

                // 获取前一个WAL的LSN
                currLSN = new LogSequenceNumber(logFileNo, prevFileEndOffset);
                fileOffset = currLSN.getFileOffset();
            }

            if (currLSN.compareTo(recoveryInfo.firstLSN) <= 0) {
                break;
            }

            if (oldLSN == null || oldLSN.getLogFileNo() != logFileNo) {
                // 文件不一致则重新获取Reader
                walReader = getWALFileReader(currLSN);
            }

            // WAL记录的最后一个字节都是type,向前移动1byte便于读取typeId
            walReader.movePosition(-1);
            byte typeID = walReader.readByte();
            WALRecordType type = WALRecordType.valueOf(typeID);

            // 计算此record的start,注意fileOffset指向了nextLSN的第一个字节
            int startOffset;
            switch (type) {
                case START_TXN:
                    // Type (1B) + TransactionID (4B) + Type (1B)
                    //startOffset指向了TransactionID的第一个字节
                    startOffset = fileOffset - 6 + 1;
                    break;

                case COMMIT_TXN:
                case ABORT_TXN:
                    // Type(1B)+TransactionID(4B)+PrevLSN(6B)+Type(1B)
                    startOffset = fileOffset - 12 + 1;
                    break;

                case UPDATE_PAGE:
                case UPDATE_PAGE_REDO_ONLY:
                    // startOffset(4B)+Type(1B)
                    walReader.movePosition(-5);
                    startOffset = walReader.readInt();
                    break;

                default:
                    throw new WALFileException("Encountered unrecognized WAL record type " + type + " at LSN " + currLSN
                            + " during redo processing!");
            }

            // 构造前一条日志的LSN，如果比要恢复的小就不处理了
            currLSN = new LogSequenceNumber(logFileNo, startOffset);
            if (currLSN.compareTo(recoveryInfo.firstLSN) < 0) {
                break;
            }

            // 读取transactionID，若此事务已完成，则不处理
            int transactionID = walReader.readInt();
            if (recoveryInfo.isTxnComplete(transactionID)) {
                oldLSN = currLSN;
                continue;
            }

            //开始执行回滚操作
            logger.debug(
                    String.format("Undoing WAL record at %s.  Type = %s, TxnID = %d", currLSN, type, transactionID));

            switch (type) {
                case START_TXN:
                    // Record that the transaction is aborted.
                    writeTxnRecord(WALRecordType.ABORT_TXN, transactionID, recoveryInfo.getLastLSN(transactionID));

                    logger.debug(String.format("Undo phase:  aborted transaction %d", transactionID));
                    recoveryInfo.recordTxnCompleted(transactionID);

                    break;

                case COMMIT_TXN:
                case ABORT_TXN:
                    // 因为现在是执行未处理玩的事务，所以不可能是处理COMMIT ABORT
                    throw new IllegalStateException("Saw a " + type
                            + "WAL-record for supposedly incomplete transaction " + transactionID + "!");

                case UPDATE_PAGE:
                    // 读取数据页
                    String undoFilename = walReader.readVarString255();
                    int undoPageNo = walReader.readUnsignedShort();
                    // 打开数据文件
                    DBFile undoFile = storageManager.openDBFile(undoFilename);
                    DBPage undoPage = storageManager.loadDBPage(undoFile, undoPageNo);

                    int numSegments = walReader.readUnsignedShort();
                    logger.debug(String.format("Undoing changes to file %s, page %d (%d segments)", undoFile,
                            undoPageNo, numSegments));

                    // 执行回退操作
                    byte[] redoOnlyData = applyUndoAndGenRedoOnlyData(walReader, undoPage, numSegments);

                    // Update the WAL with the redo-only record. Make sure to
                    // grab
                    // the LSN of the redo-only record so that we can chain any
                    // subsequent records after this one.
                    // 将回退操作的内容也记录到新的日志中
                    LogSequenceNumber redoOnlyLSN = writeRedoOnlyUpdatePageRecord(transactionID,
                            recoveryInfo.getLastLSN(transactionID), undoPage, numSegments, redoOnlyData);

                    recoveryInfo.updateInfo(transactionID, redoOnlyLSN);

                    break;

                case UPDATE_PAGE_REDO_ONLY:
                    // We ignore redo-only updates during the undo phase.
                    break;

                default:
                    throw new WALFileException("Encountered unrecognized WAL record type " + type + " at LSN " + currLSN
                            + " during undo processing!");
            }

            oldLSN = currLSN;
        }

        logger.debug("Undo processing is complete.");
    }

    private LogSequenceNumber computeNextLSN(int fileNo, int fileOffset) {
        if (fileOffset >= MAX_WAL_FILE_SIZE) {
            // WAL文件超过大小限制后，fileNo+1,offset重置
            fileNo += 1;
            if (fileNo > MAX_WAL_FILE_NUMBER) {
                fileNo = 0;
            }
            fileOffset = OFFSET_FIRST_RECORD;
        }
        return new LogSequenceNumber(fileNo, fileOffset);
    }

    /**
     * This method opens the WAL file specified in the passed-in Log Sequence
     * Number, wraps it with a {@link DBFileWriter} so that it can be read and
     * written, and then seeks to the specified file offset.
     *
     * Because we are writing, the file may not yet exist. In that case, this
     * method will also create a new WAL file for the specified file number,
     * initializing it with the proper values, and then seeking to the location
     * where the first WAL record may be written.
     *
     * @param lsn The log sequence number specifying the WAL file and the offset
     *        in the WAL file to go to.
     *
     * @return the WAL file, with the file position moved to the specified
     *         offset.
     *
     * @throws IOException if the corresponding WAL file cannot be opened, or if
     *         some other IO error occurs.
     */
    private DBFileWriter getWALFileWriter(LogSequenceNumber lsn) throws IOException {

        int fileNo = lsn.getLogFileNo();
        int offset = lsn.getFileOffset();

        DBFile walFile;
        try {
            walFile = openWALFile(fileNo);
        } catch (FileNotFoundException e) {
            logger.debug("WAL file doesn't exist!  WAL is expanding into a new file.");
            walFile = createWALFile(fileNo);
            // TODO: Write the previous WAL file's last file-offset into the new
            // WAL file's start.
        }

        DBFileWriter writer = new DBFileWriter(walFile);
        writer.setPosition(offset);

        return writer;
    }

    /**
     * 根据LSN找到对应的日志文件，移动到指定位置
     * 
     * @param lsn fileNo+offset,指定要读取的日志的起始位置
     * @return 从指定位置开始的一个DBFileReader
     * @throws IOException e
     */
    private DBFileReader getWALFileReader(LogSequenceNumber lsn) throws IOException {

        int fileNo = lsn.getLogFileNo();
        int offset = lsn.getFileOffset();

        DBFile walFile = openWALFile(fileNo);
        DBFileReader reader = new DBFileReader(walFile);
        reader.setPosition(offset);

        return reader;
    }

    /**
     * 写一个事务分界点到日志中
     *
     * @see WALRecordType#START_TXN
     * @see WALRecordType#COMMIT_TXN
     * @see WALRecordType#ABORT_TXN
     * @param type WALRecordType枚举
     * @return 写入到日志中的内容对应的LSN
     * @throws IOException the write-ahead log 不能更新
     * @throws IllegalStateException 当前没有事务
     */
    public LogSequenceNumber writeTxnRecord(WALRecordType type) throws IOException {

        // Retrieve and verify the transaction state.
        TransactionState txnState = SessionState.get().getTxnState();
        if (!txnState.isTxnInProgress()) {
            throw new IllegalStateException("No transaction is currently in progress!");
        }

        LogSequenceNumber lsn = writeTxnRecord(type, txnState.getTransactionID(), txnState.getLastLSN());

        txnState.setLastLSN(lsn);

        return lsn;
    }

    /**
     * 写一个事务分界点到日志中,正常情况下应该用{@link #writeTxnRecord(WALRecordType)}
     * 
     * @param type 日志类型
     * @param transactionID 事务ID
     * @param prevLSN 前一个LSN
     * @return 当前日志操作的LSN
     * @throws IOException e
     */
    public LogSequenceNumber writeTxnRecord(WALRecordType type, int transactionID, LogSequenceNumber prevLSN)
            throws IOException {

        if (type != WALRecordType.START_TXN && type != WALRecordType.COMMIT_TXN && type != WALRecordType.ABORT_TXN) {
            throw new IllegalArgumentException("Invalid record type " + type + " passed to writeTxnRecord().");
        }

        if ((type == WALRecordType.COMMIT_TXN || type == WALRecordType.ABORT_TXN) && prevLSN == null) {
            throw new IllegalArgumentException("prevLSN must be specified for records of type " + type);
        }

        LogSequenceNumber lsn = nextLSN;

        logger.debug("Writing a " + type + " record for transaction " + transactionID + " at LSN " + lsn);

        // Record the WAL record. First thing to do: figure out where it goes.

        DBFileWriter walWriter = getWALFileWriter(lsn);

        walWriter.writeByte(type.getID());
        walWriter.writeInt(transactionID);

        if (type == WALRecordType.START_TXN) {
            walWriter.writeByte(type.getID());

            // TypeID (1B) + TransactionID (4B) + TypeID (1B)
            lsn.setRecordSize(6);
        } else {
            walWriter.writeShort(prevLSN.getLogFileNo());
            walWriter.writeInt(prevLSN.getFileOffset());
            walWriter.writeByte(type.getID());

            // TypeID (1B) + TransactionID (4B) + PrevLSN (6B) + TypeID (1B)
            lsn.setRecordSize(12);
        }

        nextLSN = computeNextLSN(nextLSN.getLogFileNo(), walWriter.getPosition());
        logger.debug("Next-LSN value is now " + nextLSN);

        return lsn;
    }

    /**
     * 将dbPage里的新数据写到write-ahead log中，包括undo and redo details.
     * 
     * @param dbPage 数据页
     * @return 本次记录日志的LogSequenceNumber
     * @throws IOException e
     */
    public LogSequenceNumber writeUpdatePageRecord(DBPage dbPage) throws IOException {

        if (dbPage == null)
            throw new IllegalArgumentException("dbPage must be specified");

        if (!dbPage.isDirty())
            throw new IllegalArgumentException("dbPage has no updates to store");

        // Retrieve and verify the transaction state.
        TransactionState txnState = SessionState.get().getTxnState();
        if (!txnState.isTxnInProgress()) {
            throw new IllegalStateException("No transaction is currently in progress!");
        }

        LogSequenceNumber lsn = nextLSN;

        logger.debug(String.format("Writing an %s record for transaction %d at LSN %s", WALRecordType.UPDATE_PAGE,
                txnState.getTransactionID(), lsn));

        // Record the WAL record. First thing to do: figure out where it goes.

        DBFileWriter walWriter = getWALFileWriter(lsn);

        walWriter.writeByte(WALRecordType.UPDATE_PAGE.getID());
        walWriter.writeInt(txnState.getTransactionID());

        // We need to store the previous log sequence number for this record.
        LogSequenceNumber prevLSN = txnState.getLastLSN();
        walWriter.writeShort(prevLSN.getLogFileNo());
        walWriter.writeInt(prevLSN.getFileOffset());

        walWriter.writeVarString255(dbPage.getDBFile().getDataFile().getName());
        walWriter.writeShort(dbPage.getPageNo());

        // This offset is where we will store the number of data segments we
        // need to record.
        int segCountOffset = walWriter.getPosition();
        walWriter.writeShort(-1);

        byte[] oldData = dbPage.getOldPageData();
        byte[] newData = dbPage.getPageData();
        int pageSize = dbPage.getPageSize();

        /***
         * DEBUG: Show the contents of the old and new pages. This is *really*
         * useful debugging code, so I don't want to throw it away, but I don't
         * rightly know where or how to enable/disable it. int i = 0; while (i <
         * pageSize) { boolean same = true; for (int j = 0; j < 32; j++) { if
         * (oldData[i + j] != newData[i + j]) { same = false; break; } }
         * 
         * if (!same) { System.err.printf("%04X OLD: ", i); for (int j = 0; j <
         * 32; j++) System.err.printf(" %02X", oldData[i + j]);
         * System.err.println();
         * 
         * System.err.printf("%04X NEW: ", i); for (int j = 0; j < 32; j++) { if
         * (newData[i + j] != oldData[i + j]) System.err.printf(" %02X",
         * newData[i + j]); else System.err.print(" .."); }
         * System.err.println(); }
         * 
         * i += 32; }
         */

        int numSegments = 0;
        int index = 0;
        while (index < pageSize) {
            logger.debug("Skipping identical bytes starting at index " + index);

            // Skip data until we find stuff that's different.
            index += ArrayUtil.sizeOfIdenticalRange(oldData, newData, index);
            assert index <= pageSize;
            if (index == pageSize)
                break;

            logger.debug("Recording changed bytes starting at index " + index);

            // Find out how much data is actually changed. We lump in small
            // runs of unchanged data just to make things more efficient.
            int size = 0;
            while (index + size < pageSize) {
                size += ArrayUtil.sizeOfDifferentRange(oldData, newData, index + size);
                assert index + size <= pageSize;
                if (index + size == pageSize)
                    break;

                // If there are 4 or less identical bytes after the different
                // bytes, include them in this segment.
                int sameSize = ArrayUtil.sizeOfIdenticalRange(oldData, newData, index + size);

                if (sameSize > 4 || index + size + sameSize == pageSize)
                    break;

                size += sameSize;
            }

            logger.debug("Found " + size + " changed bytes starting at index " + index);

            // Write the starting index within the page, and the amount of
            // data that will be recorded at that index.
            walWriter.writeShort(index);
            walWriter.writeShort(size);

            // Write the old data (undo), and then the new data (redo).
            walWriter.write(oldData, index, size);
            walWriter.write(newData, index, size);

            numSegments++;

            index += size;
        }
        assert index == pageSize;

        // Now that we know how many segments were recorded, store that value
        // at the appropriate location.
        int currOffset = walWriter.getPosition();
        walWriter.setPosition(segCountOffset);
        walWriter.writeShort(numSegments);
        walWriter.setPosition(currOffset);

        // Write the start of the update record at the end so that we can get
        // back to the record's start when scanning the log backwards.

        walWriter.writeInt(lsn.getFileOffset());
        walWriter.writeByte(WALRecordType.UPDATE_PAGE.getID());

        // Store the LSN of the change on the page.
        lsn.setRecordSize(walWriter.getPosition() - lsn.getFileOffset());
        dbPage.setPageLSN(lsn);

        txnState.setLastLSN(lsn);

        nextLSN = computeNextLSN(nextLSN.getLogFileNo(), walWriter.getPosition());

        return lsn;
    }

    /**
     * 从WAL中读取数据重做到数据文件dbPage中
     * 
     * @param type WAL操作类型
     * @param walReader 从WAL中读取日志的reader
     * @param dbPage 重做的数据页
     * @param numSegments 要重做的数据段数
     * @throws IOException e
     */
    private void applyRedo(WALRecordType type, DBFileReader walReader, DBPage dbPage, int numSegments)
            throws IOException {

        for (int iSeg = 0; iSeg < numSegments; iSeg++) {
            // 获取在数据页的位置和重做数据大小
            int index = walReader.readUnsignedShort();
            int size = walReader.readUnsignedShort();

            // If it's an UPDATE_PAGE record, skip over the undo data.
            // UPDATE_PAGE 会先写undo data然后再写redo data
            if (type == WALRecordType.UPDATE_PAGE) {
                walReader.movePosition(size);
            }

            // 从WAL中获取重做数据，写入到数据页中
            byte[] redoData = new byte[size];
            walReader.read(redoData);
            dbPage.write(index, redoData);
        }
    }

    /**
     * 执行Undo并将回滚的数据返回
     * @param walReader 日志reader
     * @param dbPage 要回滚的数据页
     * @param numSegments 回滚的数据段个数
     * @return undo的数据
     * @throws IOException e
     */
    private byte[] applyUndoAndGenRedoOnlyData(DBFileReader walReader, DBPage dbPage, int numSegments)
            throws IOException {

        ByteArrayOutputStream redoOnlyBAOS = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(redoOnlyBAOS);

        for (int i = 0; i < numSegments; i++) {
            // Read the starting index and length of this segment.
            int start = walReader.readUnsignedShort();
            int length = walReader.readUnsignedShort();

            // 对数据页执行undo.
            byte[] undoData = new byte[length];
            walReader.read(undoData);
            dbPage.write(start, undoData);

            // 跳过 redo data, 在undo数据后面接着就是redo数据.
            walReader.movePosition(length);

            // Record what we wrote into the redo-only record data.
            dos.writeShort(start);
            dos.writeShort(length);
            dos.write(undoData);
        }

        // Return the data that will appear in the redo-only record body.
        dos.flush();
        return redoOnlyBAOS.toByteArray();
    }

    /**
     * This method writes a redo-only update-page record to the write-ahead log,
     * including only redo details. The transaction state is passed explicitly
     * so that this method can be used during recovery processing. The alternate
     * method {@link #writeRedoOnlyUpdatePageRecord(DBPage, int, byte[])}
     * retrieves the transaction state from thread-local storage, and should be
     * used during normal operation.
     *
     * @param transactionID the transaction ID that the WAL record is for.
     *
     * @param prevLSN the log sequence number of the transaction's immediately
     *        previous WAL record.
     *
     * @param dbPage The data page whose changes are to be recorded in the log.
     *
     * @param numSegments The number of segments in the change-data to record.
     *
     * @param changes The actual changes themselves, serialized to a byte array.
     * 
     * @return the Log Sequence Number of the WAL record that was written
     *
     * @throws IOException if the write-ahead log cannot be updated for some
     *         reason.
     *
     * @throws IllegalArgumentException if <tt>dbPage</tt> is <tt>null</tt>, or
     *         if <tt>changes</tt> is <tt>null</tt>.
     */
    public LogSequenceNumber writeRedoOnlyUpdatePageRecord(int transactionID, LogSequenceNumber prevLSN, DBPage dbPage,
            int numSegments, byte[] changes) throws IOException {

        if (dbPage == null)
            throw new IllegalArgumentException("dbPage must be specified");

        if (changes == null)
            throw new IllegalArgumentException("changes must be specified");

        // Record the WAL record. First thing to do: figure out where it goes.

        LogSequenceNumber lsn = nextLSN;

        logger.debug(String.format("Writing redo-only update record for " + "transaction %d at LSN %s.  PrevLSN = %s",
                transactionID, lsn, prevLSN));

        DBFileWriter walWriter = getWALFileWriter(lsn);

        walWriter.writeByte(WALRecordType.UPDATE_PAGE_REDO_ONLY.getID());
        walWriter.writeInt(transactionID);

        // We need to store the previous log sequence number for this record.
        walWriter.writeShort(prevLSN.getLogFileNo());
        walWriter.writeInt(prevLSN.getFileOffset());

        walWriter.writeVarString255(dbPage.getDBFile().getDataFile().getName());
        walWriter.writeShort(dbPage.getPageNo());

        // Write the redo-only data.
        walWriter.writeShort(numSegments);
        walWriter.write(changes);

        // Write the start of the update record at the end so that we can get
        // back to the record's start when scanning the log backwards.

        walWriter.writeInt(lsn.getFileOffset());
        walWriter.writeByte(WALRecordType.UPDATE_PAGE_REDO_ONLY.getID());

        // Store the LSN of the change on the page.
        lsn.setRecordSize(walWriter.getPosition() - lsn.getFileOffset());
        dbPage.setPageLSN(lsn);

        nextLSN = computeNextLSN(nextLSN.getLogFileNo(), walWriter.getPosition());

        return lsn;
    }

    public LogSequenceNumber writeRedoOnlyUpdatePageRecord(DBPage dbPage, int numSegments, byte[] changes)
            throws IOException {

        // Retrieve and verify the transaction state.
        TransactionState txnState = SessionState.get().getTxnState();
        if (!txnState.isTxnInProgress()) {
            throw new IllegalStateException("No transaction is currently in progress!");
        }

        LogSequenceNumber lsn = writeRedoOnlyUpdatePageRecord(txnState.getTransactionID(), txnState.getLastLSN(),
                dbPage, numSegments, changes);

        txnState.setLastLSN(lsn);

        return lsn;
    }

    /**
     * This method performs the operations necessary to rollback the current
     * transaction from the database. The transaction details are taken from the
     * transaction state stored in thread-local storage. This method is not used
     * during recovery processing; the {@link #performUndo} method is used to
     * rollback all incomplete transactions in the logs.
     *
     * @throws IOException if an IO error occurs during rollback.
     */
    public void rollbackTransaction() throws IOException {
        // Get the details for the transaction to rollback.
        TransactionState txnState = SessionState.get().getTxnState();

        int transactionID = txnState.getTransactionID();
        if (transactionID == TransactionState.NO_TRANSACTION) {
            logger.info("No transaction in progress - rollback is a no-op.");
            return;
        }

        LogSequenceNumber lsn = txnState.getLastLSN();

        logger.info("Rolling back transaction " + transactionID + ".  Last LSN = " + lsn);

        // Scan backward through the log records for this transaction to roll
        // it back.

        while (true) {
            DBFileReader walReader = getWALFileReader(lsn);

            WALRecordType type = WALRecordType.valueOf(walReader.readByte());
            int recordTxnID = walReader.readInt();
            if (recordTxnID != transactionID) {
                throw new WALFileException(String.format(
                        "Sent to WAL record " + "for transaction %d at LSN %s, during rollback of " + "transaction %d.",
                        recordTxnID, lsn, transactionID));
            }

            logger.debug(String.format("Undoing WAL record at %s.  Type = %s, TxnID = %d", lsn, type, transactionID));

            if (type == WALRecordType.START_TXN) {
                // Done rolling back the transaction.
                logger.debug("Hit the start of the transaction, rollback done.");
                break;
            }

            // Read out the "previous LSN" value.
            int prevFileNo = walReader.readUnsignedShort();
            int prevOffset = walReader.readInt();
            LogSequenceNumber prevLSN = new LogSequenceNumber(prevFileNo, prevOffset);

            logger.debug("Read PrevLSN of " + prevLSN);

            if (type == WALRecordType.UPDATE_PAGE) {
                // Undo this change.

                // Read the file and page with the changes to undo.
                String filename = walReader.readVarString255();
                int pageNo = walReader.readUnsignedShort();

                // Open the specified file and retrieve the data page to undo.
                DBFile dbFile = storageManager.openDBFile(filename);
                DBPage dbPage = storageManager.loadDBPage(dbFile, pageNo);

                // Read the number of segments in the redo/undo record, and
                // undo the writes. While we do this, the data for a redo-only
                // record is also accumulated.
                int numSegments = walReader.readUnsignedShort();

                logger.debug(
                        String.format("UPDATE_PAGE record is for file " + "%s, page %d.  Record contains %d segments.",
                                filename, pageNo, numSegments));

                byte[] redoOnlyData = applyUndoAndGenRedoOnlyData(walReader, dbPage, numSegments);

                logger.debug("Generated " + redoOnlyData.length + " bytes of redo-only data.");

                // Finally, update the WAL with the redo-only record. The
                // method takes care of setting the DBPage's PageLSN value.
                writeRedoOnlyUpdatePageRecord(dbPage, numSegments, redoOnlyData);
            } else {
                logger.warn(String.format(
                        "Encountered unexpected WAL-record " + "type %s while rolling back transaction %d.", type,
                        transactionID));
            }

            // Go to the immediately preceding record in the logs for this
            // transaction.
            lsn = prevLSN;
        }

        // All done rolling back the transaction! Record that it was aborted
        // in the WAL.
        writeTxnRecord(WALRecordType.ABORT_TXN);
        logger.info(String.format("Transaction %d:  Rollback complete.", transactionID));
    }
}
