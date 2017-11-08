package com.bow.lab.storage;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.commands.ExecutionException;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.TableSchema;
import edu.caltech.nanodb.storage.DBFileType;
import edu.caltech.nanodb.storage.StorageManager;
import edu.caltech.nanodb.storage.TableFileInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wwxiang
 * @since 2017/11/8.
 */
public class CreateTableCmd extends Command {

    private String tableName;

    private List<ColumnInfo> columnInfos = new ArrayList<ColumnInfo>();

    public CreateTableCmd() {
        super(Type.DDL);
    }

    @Override
    public void execute() throws ExecutionException {
        StorageManager storageManager = StorageManager.getInstance();
        TableFileInfo tblFileInfo = new TableFileInfo(tableName);
        tblFileInfo.setFileType(DBFileType.HEAP_DATA_FILE);
        TableSchema schema = tblFileInfo.getSchema();
        for (ColumnInfo colInfo : columnInfos) {
            try {
                schema.addColumnInfo(colInfo);
            } catch (IllegalArgumentException iae) {
                throw new ExecutionException("Duplicate or invalid column \"" + colInfo.getName() + "\".", iae);
            }
        }
        try {
            storageManager.createTable(tblFileInfo);
        } catch (IOException ioe) {
            throw new ExecutionException("Can't create table "+tableName, ioe);
        }
    }

    public void addColumn(ColumnInfo colInfo) {
        this.columnInfos.add(colInfo);
    }


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public void setColumnInfos(List<ColumnInfo> columnInfos) {
        this.columnInfos = columnInfos;
    }
}
