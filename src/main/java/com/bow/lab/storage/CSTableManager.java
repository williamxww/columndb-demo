package com.bow.lab.storage;

import edu.caltech.nanodb.relations.Tuple;
import edu.caltech.nanodb.storage.BlockedTableReader;
import edu.caltech.nanodb.storage.FilePointer;
import edu.caltech.nanodb.storage.InvalidFilePointerException;
import edu.caltech.nanodb.storage.TableFileInfo;
import edu.caltech.nanodb.storage.TableManager;

import java.io.IOException;
import java.util.Map;

/**
 * @author vv
 * @since 2017/11/7.
 */
public class CSTableManager implements TableManager {
    @Override
    public void initTableInfo(TableFileInfo tblFileInfo) throws IOException {

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
