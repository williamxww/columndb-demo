package edu.caltech.nanodb.commands;

import edu.caltech.nanodb.storage.FileAnalyzer;
import edu.caltech.nanodb.storage.StorageManager;
import edu.caltech.nanodb.storage.TableFileInfo;
import edu.caltech.nanodb.storage.colstore.ColStoreTableManager;

import java.io.IOException;

/**
 * 从文本文件中加载数据
 * 
 * @author vv
 * @since 2017/10/22.
 */
public class LoadFileCommand extends Command {

    private String tableName;

    private String fileName;

    public LoadFileCommand(String tableName, String fileName) {
        super(Command.Type.DDL);
        if (tableName == null) {
            throw new IllegalArgumentException("TableName cannot be null");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("FileName cannot be null");
        }
        this.tableName = tableName;
        this.fileName = fileName;
    }

    @Override
    public void execute() throws ExecutionException {

        // 获取导入表的信息
        TableFileInfo tableFileInfo;
        try {
            tableFileInfo = StorageManager.getInstance().openTable(tableName);
        } catch (IOException e) {
            throw new ExecutionException("Failed to get table file info for table " + tableName);
        }

        // 解析要导入的文件
        FileAnalyzer analyzer;
        try {
            analyzer = new FileAnalyzer(fileName);
            analyzer.analyze(tableFileInfo.getSchema().getColumnInfos());
        } catch (IOException e) {
            throw new ExecutionException("There was an error analyzing the data file.");
        }

        // 导入文件
        try {
            ColStoreTableManager tableManager = (ColStoreTableManager) tableFileInfo.getTableManager();
            tableManager.writeTable(analyzer, tableFileInfo);
        } catch (IOException e) {
            throw new ExecutionException("Could not write to table " + tableName + ".  See nested exception ", e);
        } catch (InterruptedException e) {
            throw new ExecutionException("Could not write to table " + tableName + ".  See nested exception ", e);
        }
    }
}
