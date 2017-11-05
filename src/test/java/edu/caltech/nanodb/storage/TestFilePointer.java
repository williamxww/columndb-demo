package edu.caltech.nanodb.storage;

import org.junit.Test;

/**
 * This class exercises the functionality of the {@link FilePointer} class.
 */
public class TestFilePointer {

    @Test
    public void testConstructor() {
        FilePointer fptr = new FilePointer(15, 103);
        assert fptr.getPageNo() == 15;
        assert fptr.getOffset() == 103;

        try {
            FilePointer fptr2 = new FilePointer(-6, 523);
            assert (false);
        } catch (IllegalArgumentException e) {
            // Success.
        }

        try {
            FilePointer fptr3 = new FilePointer(523, -6);
            assert (false);
        } catch (IllegalArgumentException e) {
            // Success.
        }
    }

    @Test
    public void testEqualsHashCode() {
        FilePointer fp1 = new FilePointer(35, 21);
        FilePointer fp2 = new FilePointer(35, 22);
        FilePointer fp3 = new FilePointer(36, 21);
        FilePointer fp4 = new FilePointer(35, 21);

        assert fp1.equals(fp4);
        assert fp4.equals(fp1);

        assert fp1.hashCode() == fp4.hashCode();
        assert fp4.hashCode() == fp1.hashCode();

        assert !fp1.equals(fp2);
        assert !fp1.equals(fp3);
        assert !fp2.equals(fp1);
        assert !fp3.equals(fp1);

        // This is not required by the general contract of hashCode(), but it
        // should be true for our specific implementation.
        assert fp1.hashCode() != fp2.hashCode();
        assert fp1.hashCode() != fp3.hashCode();
        assert fp2.hashCode() != fp3.hashCode();
    }

    @Test
    public void testCompare() {
        FilePointer fp1 = new FilePointer(35, 21);
        FilePointer fp2 = new FilePointer(35, 22);
        FilePointer fp3 = new FilePointer(36, 21);
        FilePointer fp4 = new FilePointer(36, 22);
        FilePointer fp5 = new FilePointer(35, 21);

        assert fp1.compareTo(fp2) < 0;
        assert fp2.compareTo(fp1) > 0;

        assert fp1.compareTo(fp3) < 0;
        assert fp3.compareTo(fp1) > 0;

        assert fp1.compareTo(fp4) < 0;
        assert fp4.compareTo(fp1) > 0;

        assert fp1.compareTo(fp5) == 0;
        assert fp5.compareTo(fp1) == 0;
    }
}
