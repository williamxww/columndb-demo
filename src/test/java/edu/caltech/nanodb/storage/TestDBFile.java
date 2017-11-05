package edu.caltech.nanodb.storage;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * This class exercises some of the core utility methods of the {@link DBFile}
 * class.
 */
public class TestDBFile {

    /**
     * 有效的page size是大于512，小于65536，且是2的N次幂
     */
    @Test
    public void testIsValidPageSize() {
        // This is too small.
        assert !DBFile.isValidPageSize(256);

        // This is too large.
        assert !DBFile.isValidPageSize(131072);

        // These are not a power of 2.
        assert !DBFile.isValidPageSize(513);

        // These are valid sizes.
        assert DBFile.isValidPageSize(512);
        assert DBFile.isValidPageSize(1024);
        assert DBFile.isValidPageSize(65536);
    }

    @Test
    public void testCheckValidPageSize() {
        DBFile.checkValidPageSize(65536);
        DBFile.checkValidPageSize(512);

        try {
            DBFile.checkValidPageSize(8000);
            assert false;
        } catch (IllegalArgumentException e) {
            // Success!
        }

        try {
            DBFile.checkValidPageSize(10000);
            assert false;
        } catch (IllegalArgumentException e) {
            // Success!
        }
    }

    /**
     * {@link DBFile#encodePageSize(int)},求对数(log N)
     */
    @Test
    public void testEncodePageSize() {
        Assert.assertTrue(DBFile.encodePageSize(512) == 9);
        Assert.assertTrue(DBFile.encodePageSize(1024) == 10);
        Assert.assertTrue(DBFile.encodePageSize(65536) == 16);
    }

    /**
     * 求2的N次幂
     */
    @Test
    public void testDecodePageSize() {
        Assert.assertTrue(DBFile.decodePageSize(10) == 1024);
    }

    /**
     * 同一个文件块是相等的
     * 
     * @throws IOException e
     */
    @Test
    public void testEqualsHashCode() throws IOException {
        File f1 = getTempFile();
        File f2 = getTempFile();

        DBFile dbf1 = new DBFile(f1, DBFileType.HEAP_DATA_FILE, DBFile.DEFAULT_PAGESIZE);
        DBFile dbf2 = new DBFile(f1, DBFileType.HEAP_DATA_FILE, DBFile.DEFAULT_PAGESIZE);
        assert dbf1.equals(dbf2);
        assert dbf2.equals(dbf1);
        assert dbf1.hashCode() == dbf2.hashCode();

        DBFile dbf3 = new DBFile(f2, DBFileType.HEAP_DATA_FILE, DBFile.DEFAULT_PAGESIZE);
        assert !dbf1.equals(dbf3);
        assert dbf1.hashCode() != dbf3.hashCode();
    }

    private File getTempFile() throws IOException {
        File tmp = File.createTempFile("tmp", null);
        tmp.deleteOnExit();
        return tmp;
    }
}
