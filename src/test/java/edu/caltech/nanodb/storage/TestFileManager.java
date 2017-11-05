package edu.caltech.nanodb.storage;


import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;


public class TestFileManager extends StorageTestCase {

    private FileManager fileMgr;


    @Before
    public void beforeClass() {
        fileMgr = new FileManager(testBaseDir);
    }


    @Test
    public void testCreateDeleteFile() throws IOException {
        String filename = "TestFileManager_testCreateDeleteFile";
        File f = new File(testBaseDir, filename);
        if (f.exists())
            f.delete();

        // 创建一个数据库文件，初始化时只有1个page
        DBFile dbf = fileMgr.createDBFile(filename, DBFileType.HEAP_DATA_FILE,
            DBFile.DEFAULT_PAGESIZE);
        
        f = dbf.getDataFile();
        assert f.getName().equals(filename);
        assert f.length() == DBFile.DEFAULT_PAGESIZE;
        assert f.canRead();

        DBPage page0 = fileMgr.loadDBPage(dbf, 0);
        assert page0.readByte(0) == DBFileType.HEAP_DATA_FILE.getID();
        assert DBFile.decodePageSize(page0.readByte(1)) == DBFile.DEFAULT_PAGESIZE;

        fileMgr.deleteDBFile(dbf);
        assert !f.exists();
    }
    

    @Test
    public void testDoubleCreateFile() throws IOException {
        String filename = "TestFileManager_testDoubleCreateFile";
        File f = new File(testBaseDir, filename);
        if (f.exists())
            f.delete();

        DBFile dbf = fileMgr.createDBFile(filename, DBFileType.HEAP_DATA_FILE,
            DBFile.DEFAULT_PAGESIZE);

        f = dbf.getDataFile();
        assert f.getName().equals(filename);
        assert f.length() == DBFile.DEFAULT_PAGESIZE;
        assert f.canRead();

        try {
            DBFile dbf2 = fileMgr.createDBFile(filename,
                DBFileType.HEAP_DATA_FILE, DBFile.DEFAULT_PAGESIZE);

            assert false : "Shouldn't be able to create a DBFile twice.";
        }
        catch (IOException e) {
            // Success.
        }

        fileMgr.deleteDBFile(dbf);
        assert !f.exists();
    }
}
