package edu.caltech.test.nanodb.storage.heapfile;

import java.io.IOException;


import edu.caltech.nanodb.storage.DBFile;
import edu.caltech.nanodb.storage.DBFileType;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.FileManager;

import edu.caltech.nanodb.storage.heapfile.DataPage;

import edu.caltech.test.nanodb.storage.StorageTestCase;
import edu.caltech.test.nanodb.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * This class exercises some of the core utility methods of the {@link DataPage}
 * class.
 */
public class TestDataPage extends StorageTestCase {
	
	/**
	 * Keeps an instance of dbPage to be accessed by each test method after
	 * setup. A new dbPage will be created before each test method.
	 */
	private DBPage dbPage;

	
	/**
	 * dbFile is instantiated once for the class.
	 */
	private DBFile dbFile;

	
	/**
	 * A file with this name will be temporarily created under ./datafiles
	 * directory.
	 */
	private final String TEST_FILE_NAME = "TestDataPage_TestFile";


    /** This is the file-manager instance used for the tests in this class. */
    private FileManager fileMgr;


    /**
     * This set-up method initializes the file manager, data-file, and page that
     * all tests will run against.
     */
    @Before
    public void beforeClass() throws IOException {

        fileMgr = new FileManager(testBaseDir);

        // Get DBFile
        DBFileType type = DBFileType.HEAP_DATA_FILE;

        try {
            int pageSize = DBFile.DEFAULT_PAGESIZE; // 8k
            dbFile = fileMgr.createDBFile(TEST_FILE_NAME, type, pageSize);
        }
        catch (IOException e) {
            // The file is already created
        }

        dbPage = new DBPage(dbFile, 0);
        DataPage.initNewPage(dbPage);
    }
	
	
	/**
	 * Remove the dbFile created in beforeClass().
	 * 
	 * @throws IOException
	 */
	@After
	public void afterClass() throws IOException {
		fileMgr.deleteDBFile(dbFile);
	}
	

	/**
	 * Test DataPage.insertTupleDataRange().<br/>
	 * |   2 byte   | 2 byte       | 2 byte       |.........|1 byte   |1 byte   |<br/>
	 * |   slotNum  |slot1's offset|slot2's offset|.........|s1 value |s1 value |<br/>
	 *
	 */
	@Test
	public void testInsertTupleDataRange(){
		
		// (i) create two adjacent canary bytes that are at the end of the dbPage and
		// (ii) insert 4 bytes at the end of dbPage to
		// test if insertTupleDataRange() correctly slide the canary tuple.
		// (iii) insert 5 bytes between canary1 and canary2 as the second
		// test for insertTupleDataRange().
		
        int slot = 0;	// the first (0th) slots point to the canary byte
        int numSlots = DataPage.getNumSlots(dbPage);
        assert numSlots == 0;
        
        // The last index in the data array for dbPage
        int endIndex = DataPage.getTupleDataEnd(dbPage) - 1; // should be 8191
        assert endIndex == dbPage.getPageSize() - 1;
        
        // Write a canary byte (204) to the end of dbPage
        int canary1 = 204;
        int oldCanary1Index = endIndex;
        dbPage.writeByte(oldCanary1Index, canary1);
        DataPage.setNumSlots(dbPage, 1);	// we have 1 slot now.
        DataPage.setSlotValue(dbPage, slot, oldCanary1Index);

        System.out.println("--------------插入byte 204------------------");
        TestUtil.printHex(dbPage.getPageData(),true);

        // Write the second canary byte (170).
        slot = 1;		// the second (1-th) slots point to another canary byte
        int canary2 = 170;
        int oldCanary2Index = endIndex - 1;		// should be 8190
        dbPage.writeByte(oldCanary2Index, canary2);
        DataPage.setNumSlots(dbPage, 2);	// we have 2 slots now.
        DataPage.setSlotValue(dbPage, slot, oldCanary2Index);

        System.out.println("--------------插入byte 170------------------");
        TestUtil.printHex(dbPage.getPageData(),true);

        // check that the canary values are stored correctly
        int tupleDataStart = DataPage.getTupleDataStart(dbPage);
        assert tupleDataStart == 8190 ;
        
        // 在page的末尾插入4个字节 canary1，2往前移动4个字节
        int off = endIndex + 1;
        DataPage.insertTupleDataRange(dbPage, off, 4);
        System.out.println("-------------插入4byte 0-------------------");
        TestUtil.printHex(dbPage.getPageData(),true);

        
        // Check that the canary values are correctly slid forward
        int newCanary1Index = DataPage.getSlotValue(dbPage, 0); // should be 8187
        int newCanary2Index = DataPage.getSlotValue(dbPage, 1); // should be 8186
        assert newCanary1Index == oldCanary1Index - 4 ;
        assert newCanary2Index == oldCanary2Index - 4 ;
        
        // read back the canary values.
        assert dbPage.readByte(newCanary1Index) == (byte) canary1;
        assert dbPage.readByte(newCanary2Index) == (byte) canary2;
        
        
        // Now we insert 5 bytes between canary1 and canary2
        off = newCanary1Index;
        DataPage.insertTupleDataRange(dbPage, off, 5);
        
        // Check that the canary values are correctly slid forward
        newCanary1Index = DataPage.getSlotValue(dbPage, 0); // should be 8187
        newCanary2Index = DataPage.getSlotValue(dbPage, 1); // should be 8181
        assert newCanary1Index == 8187 ;
        assert newCanary2Index == 8181 ;
        
        // read back the canary values.
        assert dbPage.readByte(newCanary1Index) == (byte) canary1;
        assert dbPage.readByte(newCanary2Index) == (byte) canary2;
        
	}
	
	
	/**
	 * 在page尾部插入3个int,然后移除中间的
	 * Test DataPage.deleteTupleDataRange().
	 */
	@Test
	public void testDeleteTupleDataRange(){

        int slot = 0;
        int numSlots = DataPage.getNumSlots(dbPage);
        assert numSlots == 0;

        int pageSize = dbPage.getPageSize();
        int endIndex = DataPage.getTupleDataEnd(dbPage) - 1; // should be 8191
        assert endIndex == pageSize - 1;
        
        // 插入int 204
        int canary1 = 204;
        int oldCanary1Index = pageSize-4;	// int needs 4 bytes
        dbPage.writeInt(oldCanary1Index, canary1);
        DataPage.setNumSlots(dbPage, 1);	// we have 1 slot now.
        DataPage.setSlotValue(dbPage, slot, oldCanary1Index);

        // check the first canary values is stored correctly
        int tupleDataStart = DataPage.getTupleDataStart(dbPage);
        assert tupleDataStart == pageSize-4 ;

        System.out.println("--------插入204 int-------------");
        TestUtil.printHex(dbPage.getPageData(),true);
        
        //插入170
        slot = 1;
        int canary2 = 170;
        int oldCanary2Index = oldCanary1Index - 4;	// int needs 4 bytes
        dbPage.writeInt(oldCanary2Index, canary2);
        DataPage.setNumSlots(dbPage, 2);	// we have 2 slots now.
        DataPage.setSlotValue(dbPage, slot, oldCanary2Index);
       
        // check the second canary values is stored correctly
        tupleDataStart = DataPage.getTupleDataStart(dbPage);
        assert tupleDataStart == pageSize-8 ;
        System.out.println("--------插入170 int-------------");
        TestUtil.printHex(dbPage.getPageData(),true);


        // 插入150
        slot = 2;
        int canary3 = 150;
        int oldCanary3Index = oldCanary2Index - 4;	// int needs 4 bytes
        dbPage.writeInt(oldCanary3Index, canary3);
        DataPage.setNumSlots(dbPage, 3);	// we have 2 slots now.
        DataPage.setSlotValue(dbPage, slot, oldCanary3Index);
        
        // check the third canary values is stored correctly
        tupleDataStart = DataPage.getTupleDataStart(dbPage);
        assert tupleDataStart == pageSize-12;
        System.out.println("--------插入150 int-------------");
        TestUtil.printHex(dbPage.getPageData(),true);
        
        // 移除170
        DataPage.deleteTupleDataRange(dbPage, oldCanary2Index, 4);
        
        // update the slot value to EMPTY_SLOT for canary 2.
        DataPage.setSlotValue(dbPage, 1, DataPage.EMPTY_SLOT);

        System.out.println("---------移除170------------");
        TestUtil.printHex(dbPage.getPageData(),true);

        // Check canary3 is correctly slid
        int newCanary3Index = DataPage.getSlotValue(dbPage, 2); 
        assert newCanary3Index == (pageSize - 8) ;
        
        // Check canary1 is not affected 
        int newCanary1Index = DataPage.getSlotValue(dbPage, 0); 
        assert newCanary1Index == (pageSize - 4) ;
        assert dbPage.readInt(newCanary1Index) == canary1;
      	
	}
}
