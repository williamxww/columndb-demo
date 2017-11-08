package com.bow.lab.storage;

import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.TableSchema;
import edu.caltech.nanodb.relations.Tuple;
import edu.caltech.nanodb.storage.BlockedTableReader;
import edu.caltech.nanodb.storage.DBFile;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.FilePointer;
import edu.caltech.nanodb.storage.InvalidFilePointerException;
import edu.caltech.nanodb.storage.PageWriter;
import edu.caltech.nanodb.storage.StorageManager;
import edu.caltech.nanodb.storage.TableFileInfo;
import edu.caltech.nanodb.storage.TableManager;
import edu.caltech.nanodb.storage.heapfile.HeaderPage;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * @author vv
 * @since 2017/11/7.
 */
public class CSTableManager implements TableManager {

    private static final Logger LOGGER = Logger.getLogger(CSTableManager.class);

    private StorageManager storageManager;


    public CSTableManager(StorageManager storageManager) {
        if (storageManager == null){
            throw new IllegalArgumentException("storageManager cannot be null");
        }
        this.storageManager = storageManager;
    }


    /**
     * 根据tblFileInfo创建table文件
     * @param tblFileInfo table的描述信息
     * @throws IOException e
     */
    @Override
    public void initTableInfo(TableFileInfo tblFileInfo) throws IOException {
        String tableName = tblFileInfo.getTableName();
        DBFile dbFile = tblFileInfo.getDBFile();
        TableSchema schema = tblFileInfo.getSchema();

        DBPage headerPage = storageManager.loadDBPage(dbFile, 0);
        PageWriter hpWriter = new PageWriter(headerPage);
        hpWriter.setPosition(HeaderPage.OFFSET_NCOLS);
        hpWriter.writeByte(schema.numColumns());
        for (ColumnInfo colInfo : schema.getColumnInfos()) {
            ColumnType colType = colInfo.getType();
            //写类型
            hpWriter.writeByte(colType.getBaseType().getTypeID());

            //写列宽如VARCHAR(30)中的30
            if (colType.hasLength()) {
                hpWriter.writeShort(colType.getLength());
            }
            //写列名称
            hpWriter.writeVarString255(colInfo.getName());
        }
        int schemaSize = hpWriter.getPosition() - HeaderPage.OFFSET_NCOLS;
        headerPage.writeShort(HeaderPage.OFFSET_SCHEMA_SIZE, schemaSize);

        //写WAL
//        storageManager.logDBPageWrite(headerPage);
//        storageManager.unpinDBPage(headerPage);
    }

    @Override
    public void loadTableInfo(TableFileInfo tblFileInfo) throws IOException {

    }

    @Override
    public void beforeCloseTable(TableFileInfo tblFileInfo) throws IOException {

    }

    @Override
    public void beforeDropTable(TableFileInfo tblFileInfo) throws IOException {

    }

    @Override
    public Tuple getFirstTuple(TableFileInfo tblFileInfo) throws IOException {
        return null;
    }

    @Override
    public Tuple getNextTuple(TableFileInfo tblFileInfo, Tuple tup) throws IOException {
        return null;
    }

    @Override
    public Tuple getTuple(TableFileInfo tblFileInfo, FilePointer fptr) throws InvalidFilePointerException, IOException {
        return null;
    }

    @Override
    public Tuple addTuple(TableFileInfo tblFileInfo, Tuple tup) throws IOException {
        return null;
    }

    @Override
    public void updateTuple(TableFileInfo tblFileInfo, Tuple tup, Map<String, Object> newValues) throws IOException {

    }

    @Override
    public void deleteTuple(TableFileInfo tblFileInfo, Tuple tup) throws IOException {

    }

    @Override
    public void analyzeTable(TableFileInfo tblFileInfo) throws IOException {

    }

    @Override
    public BlockedTableReader getBlockedReader() {
        return null;
    }
}
