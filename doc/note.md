Sybase IQ
Infobright
monetDB
C-Store


http://www.makaidong.com/%E6%95%B0%E6%8D%AE%E5%BA%93/211155.shtml



一个库一个DBFile前2字节为

1 byte type | 1 byte encoded page-size |



DataPage   开始2字节表示总slot数 ，后面每个slot占用一个short





LogSequenceNumber，lsn是联系dirty page，redo log record和redo log file的纽带。在每个redo log record被拷贝到内存的log buffer时会产生一个相关联的lsn，而每个页面修改时会产生一个log record，从而每个数据库的page也会有一个相关联的lsn，这个lsn记录在每个page的header字段中。为了保证WAL（Write-Ahead-Logging）要求的逻辑，dirty page要求其关联lsn的log record已经被写入log file才允许执行flush操作



edu.caltech.nanodb.storage.writeahead.WALManager#doRecovery

firstLSN:需要恢复时，从此处开始恢复

nextLSN：需要写时，从此处接着写



```
TransactionStatePage
此page里存放下一个事务号，恢复时的起始LSN和写时的下一个LSN地址。
* -------------------------------------------------------------------------------------------
* |2 bytes|  4 bytes    |2 bytes          |  4 bytes       |2 bytes         |  4 bytes      |
* |   ??  | NEXT_TXN_ID |FIRST_LSN_FILENUM|FIRST_LSN_OFFSET|NEXT_LSN_FILENUM|NEXT_LSN_OFFSET|
* -------------------------------------------------------------------------------------------
```

LogSequenceNumber：logFileNo+offset，它指明了某次日志记录在文件中的起始位置。



txnstat.dat 只记录某次事务在日志文件(.log文件)中的起始位置（firstLSN），下次写日志的起始位置（nextLSN）和下次事务的id





```java
// pageData[position]为byte，若全为1，不加& 0xFF返回的是-1，但需要返回无符号整数
public int readUnsignedByte(int position) {
    return pageData[position] & 0xFF;
}
// 注意此处为a++
public long readUnsignedInt(int position) {
        long value = ((pageData[position++] & 0xFF) << 24)
                   | ((pageData[position++] & 0xFF) << 16)
                   | ((pageData[position++] & 0xFF) <<  8)
                   | ((pageData[position  ] & 0xFF)      );

        return value;
    }
```


```
at edu.caltech.nanodb.util.ArrayUtil.sizeOfDifferentRange(ArrayUtil.java:59)
at edu.caltech.nanodb.storage.writeahead.WALManager.writeUpdatePageRecord(WALManager.java:832)
at edu.caltech.nanodb.transactions.TransactionManager.recordPageUpdate(TransactionManager.java:271)
at edu.caltech.nanodb.storage.StorageManager.logDBPageWrite(StorageManager.java:460)
at edu.caltech.nanodb.storage.heapfile.HeapFileTableManager.addTuple(HeapFileTableManager.java:741)
at edu.caltech.nanodb.commands.InsertCommand.insertSingleRow(InsertCommand.java:209)
at edu.caltech.nanodb.commands.InsertCommand.execute(InsertCommand.java:165)
at edu.caltech.nanodb.server.NanoDBServer.doCommand(NanoDBServer.java:114)
at edu.caltech.nanodb.client.ExclusiveClient.main(ExclusiveClient.java:106)

```

创建表的日志

```


```



插入的日志

```log
CMD> INSERT INTO states VALUES (8, 'vv');
DEBUG 2017-09-16 main ExclusiveClient Parsed command:  InsertCommand[values=(8,'vv')]
DEBUG 2017-09-16 main TransactionStateUpdater Session ID:  1	Transaction state:  TxnState[no transaction]
DEBUG 2017-09-16 main TransactionStateUpdater No transaction is in progress; auto-starting one!
DEBUG 2017-09-16 main TransactionManager Starting transaction with ID 11
DEBUG 2017-09-16 main BufferManager Requested file STATES.tbl is NOT in file-cache.
DEBUG 2017-09-16 main FileManager Opened existing database file .\datafiles\STATES.tbl; type is HEAP_DATA_FILE, page size is 8192.
DEBUG 2017-09-16 main BufferManager Adding file STATES.tbl to file-cache.
DEBUG 2017-09-16 main StorageManager Opened DBFile for table STATES at path .\datafiles\STATES.tbl.
DEBUG 2017-09-16 main StorageManager Type is HEAP_DATA_FILE, page size is 8192 bytes.
DEBUG 2017-09-16 main BufferManager Requested page [STATES.tbl,0] is NOT in page-cache.
DEBUG 2017-09-16 main BufferManager Adding page [STATES.tbl,0] to page-cache.
DEBUG 2017-09-16 main BufferManager Session 1 is pinning page [STATES.tbl,0].  New pin-count is 1.
DEBUG 2017-09-16 main HeapFileTableManager Table has 2 columns.
DEBUG 2017-09-16 main HeapFileTableManager ColumnInfo[STATES.STATE_ID:INTEGER]
DEBUG 2017-09-16 main HeapFileTableManager ColumnInfo[STATES.STATE_NAME:VARCHAR(30)]
DEBUG 2017-09-16 main HeapFileTableManager Reading 0 constraints
DEBUG 2017-09-16 main HeaderPage Reading table-statistics from header page.
DEBUG 2017-09-16 main HeaderPage Read column-stat data:  nullmask=0xF, unique=-1, null=-1, min=null, max=null
DEBUG 2017-09-16 main HeaderPage Read column-stat data:  nullmask=0xF, unique=-1, null=-1, min=null, max=null
DEBUG 2017-09-16 main HeapFileTableManager TableStats[numDataPages=0, numTuples=0, avgTupleSize=0.0]
DEBUG 2017-09-16 main BufferManager Session 1 is unpinning page [STATES.tbl,0].  New pin-count is 0.
DEBUG 2017-09-16 main EventDispatcher Firing beforeRowInserted
DEBUG 2017-09-16 main HeapFileTableManager Adding new tuple of size 9 bytes.
DEBUG 2017-09-16 main BufferManager Requested page [STATES.tbl,1] is NOT in page-cache.
DEBUG 2017-09-16 main BufferManager Adding page [STATES.tbl,1] to page-cache.
DEBUG 2017-09-16 main BufferManager Session 1 is pinning page [STATES.tbl,1].  New pin-count is 1.
DEBUG 2017-09-16 main HeapFileTableManager Found space for new tuple in page 1.
DEBUG 2017-09-16 main DataPage Allocating space for new 9-byte tuple.
DEBUG 2017-09-16 main DataPage Current number of slots on page:  8
DEBUG 2017-09-16 main DataPage No empty slot available.  Adding a new slot.
DEBUG 2017-09-16 main DataPage Tuple will get slot 8.  Final number of slots:  9
DEBUG 2017-09-16 main DataPage New tuple of 9 bytes will reside at location [8082, 8091).
DEBUG 2017-09-16 main HeapFileTableManager New tuple will reside on page 1, slot 8.
DEBUG 2017-09-16 main TransactionManager Recording page-update for page 1 of file STATES.tbl
DEBUG 2017-09-16 main WALManager Writing a START_TXN record for transaction 11 at LSN LSN[00000:00000797]
DEBUG 2017-09-16 main WALManager Opening WAL file wal-00000.log
DEBUG 2017-09-16 main BufferManager Requested file wal-00000.log is NOT in file-cache.
DEBUG 2017-09-16 main FileManager Opened existing database file .\datafiles\wal-00000.log; type is WRITE_AHEAD_LOG_FILE, page size is 8192.
DEBUG 2017-09-16 main BufferManager Adding file wal-00000.log to file-cache.
DEBUG 2017-09-16 main BufferManager Requested page [wal-00000.log,0] is NOT in page-cache.
DEBUG 2017-09-16 main BufferManager Adding page [wal-00000.log,0] to page-cache.
DEBUG 2017-09-16 main BufferManager Session 1 is pinning page [wal-00000.log,0].  New pin-count is 1.
DEBUG 2017-09-16 main WALManager Next-LSN value is now LSN[00000:00000803]
DEBUG 2017-09-16 main WALManager Writing an UPDATE_PAGE record for transaction 11 at LSN LSN[00000:00000803]
DEBUG 2017-09-16 main WALManager Opening WAL file wal-00000.log
DEBUG 2017-09-16 main BufferManager Requested file wal-00000.log is in file-cache.
DEBUG 2017-09-16 main BufferManager Requested page [wal-00000.log,0] is in page-cache.
DEBUG 2017-09-16 main WALManager Skipping identical bytes starting at index 0
DEBUG 2017-09-16 main WALManager Recording changed bytes starting at index 1
DEBUG 2017-09-16 main WALManager Found 1 changed bytes starting at index 1
DEBUG 2017-09-16 main WALManager Skipping identical bytes starting at index 2
DEBUG 2017-09-16 main WALManager Recording changed bytes starting at index 18
DEBUG 2017-09-16 main WALManager Found 2 changed bytes starting at index 18
DEBUG 2017-09-16 main WALManager Skipping identical bytes starting at index 20
DEBUG 2017-09-16 main WALManager Recording changed bytes starting at index 8086
DEBUG 2017-09-16 main WALManager Found 5 changed bytes starting at index 8086
DEBUG 2017-09-16 main WALManager Skipping identical bytes starting at index 8091
DEBUG 2017-09-16 main EventDispatcher Firing afterRowInserted
DEBUG 2017-09-16 main IndexUpdater Adding tuple to indexes for table STATES
DEBUG 2017-09-16 main TransactionStateUpdater Session ID:  1	Transaction state:  TxnState[txnID=11, userStarted=false, loggedStart=true, lastLSN=LSN[00000:00000803]]
DEBUG 2017-09-16 main TransactionStateUpdater An auto-started transaction is in progress; committing it!
DEBUG 2017-09-16 main WALManager Writing a COMMIT_TXN record for transaction 11 at LSN LSN[00000:00000862]
DEBUG 2017-09-16 main WALManager Opening WAL file wal-00000.log
DEBUG 2017-09-16 main BufferManager Requested file wal-00000.log is in file-cache.
DEBUG 2017-09-16 main BufferManager Requested page [wal-00000.log,0] is in page-cache.
DEBUG 2017-09-16 main WALManager Next-LSN value is now LSN[00000:00000874]
DEBUG 2017-09-16 main BufferManager Requested file wal-00000.log is in file-cache.
INFO  2017-09-16 main BufferManager Writing all dirty pages for file wal-00000.log to disk (with sync).
DEBUG 2017-09-16 main BufferManager     Saving page [wal-00000.log,0] to disk.
DEBUG 2017-09-16 main BufferManager Syncing file wal-00000.log
INFO  2017-09-16 main FileManager Synchronizing database file to disk:  wal-00000.log
DEBUG 2017-09-16 main BufferManager Requested file txnstate.dat is in file-cache.
DEBUG 2017-09-16 main BufferManager Requested page [txnstate.dat,0] is in page-cache.
INFO  2017-09-16 main BufferManager Writing all dirty pages for file txnstate.dat to disk (with sync).
DEBUG 2017-09-16 main BufferManager     Saving page [txnstate.dat,0] to disk.
DEBUG 2017-09-16 main BufferManager Syncing file txnstate.dat
INFO  2017-09-16 main FileManager Synchronizing database file to disk:  txnstate.dat
DEBUG 2017-09-16 main TransactionManager WAL was successfully forced to LSN LSN[00000:00000874] (plus 0 bytes)
DEBUG 2017-09-16 main TransactionManager Transaction completed, resetting transaction state.
DEBUG 2017-09-16 main BufferManager Session 1 is unpinning page [txnstate.dat,0].  New pin-count is 0.
DEBUG 2017-09-16 main BufferManager Session 1 is unpinning page [STATES.tbl,1].  New pin-count is 0.
DEBUG 2017-09-16 main BufferManager Session 1 is unpinning page [wal-00000.log,0].  New pin-count is 0.
CMD> 
```



