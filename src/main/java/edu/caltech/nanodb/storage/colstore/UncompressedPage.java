package edu.caltech.nanodb.storage.colstore;

import org.apache.log4j.Logger;

import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.FileEncoding;
import edu.caltech.nanodb.storage.PageReader;
import edu.caltech.nanodb.storage.PageWriter;

/**
 * <pre>
 * |    1B  |       1B     |   4B      |  4B  |        4B       |
 * |FileType|encodePageSize|文件压缩方式 |count |NEXT_BLOCK_OFFSET|
 *
 * | 1B  |    4B     |  1B  |    4B     |
 * | 数据 | 数据偏移量  | 数据 | 数据偏移量  |
 * </pre>
 */
public class UncompressedPage {

    private static Logger logger = Logger.getLogger(UncompressedPage.class);

    public static final int ENCODING_OFFSET = 2;

    public static final int ENCODING_MARKER = FileEncoding.NONE.ordinal();

    public static final int COUNT_OFFSET = 6;

    public static final int NEXT_BLOCK_START_OFFSET = 10;

    public static final int FIRST_BLOCK_OFFSET = 14;

    /**
     * Initialize a newly allocated page. Currently this involves setting the
     * number of values to 0 and marking the page as normal page.
     *
     * @param dbPage the data page to initialize
     */
    public static void initNewPage(DBPage dbPage) {
        PageWriter uncWriter = new PageWriter(dbPage);
        uncWriter.setPosition(ENCODING_OFFSET);
        // 页压缩类型
        uncWriter.writeInt(ENCODING_MARKER);
        uncWriter.writeInt(0);
        uncWriter.writeInt(FIRST_BLOCK_OFFSET);
    }

    /**
     * 往page中写入数据
     * 
     * @param dbPage 待编辑的page
     * @param object 待写入的数据
     * @param position 数据的偏移量类似于rowId
     * @param type 列类型
     * @return 添加成功与否
     */
    public static boolean writeBlock(DBPage dbPage, String object, int position, ColumnType type) {

        PageReader uncReader = new PageReader(dbPage);
        PageWriter uncWriter = new PageWriter(dbPage);

        // 检查文件编码(压缩方式)
        uncReader.setPosition(ENCODING_OFFSET);
        if (uncReader.readInt() != ENCODING_MARKER) {
            throw new IllegalArgumentException("Wrong encoding type");
        }

        // 找到继续追加记录的位置
        uncReader.setPosition(NEXT_BLOCK_START_OFFSET);
        int writeOffset = uncReader.readInt();

        // 检查是否超过page size, +4是因为还要记录该object的offset
        if (writeOffset + DBPage.getObjectDiskSize(object, type) + 4 > dbPage.getPageSize()) {
            return false;
        }
        // 将值写入到文件
        int dataSize = dbPage.writeObject(writeOffset, type, object);
        uncWriter.setPosition(writeOffset + dataSize);
        // 此值的偏移量也记录下来
        uncWriter.writeInt(position);

        // 获取当前记录数和记录位置
        uncReader.setPosition(COUNT_OFFSET);
        int count = uncReader.readInt() + 1;
        int next_write_pos = uncWriter.getPosition();

        // 更新总记录数和下次追加记录的位置
        uncWriter.setPosition(COUNT_OFFSET);
        uncWriter.writeInt(count);
        uncWriter.writeInt(next_write_pos);

        return true;
    }

    /**
     * 从指定位置获取值
     * 
     * @param dbPage 数据页
     * @param blockStart 指定位置
     * @param colType 获取值的类型
     * @return 获取值
     */
    public static Object getBlockData(DBPage dbPage, int blockStart, ColumnType colType) {

        PageReader uncReader = new PageReader(dbPage);

        // 检查文件格式
        uncReader.setPosition(ENCODING_OFFSET);
        if (uncReader.readInt() != ENCODING_MARKER) {
            throw new IllegalArgumentException("Wrong encoding type");
        }

        // 校验获取位置是否OK
        uncReader.setPosition(NEXT_BLOCK_START_OFFSET);
        if (uncReader.readInt() <= blockStart) {
            return null;
        }
        return dbPage.readObject(blockStart, colType);
    }

    /** Reads first block from disk. */
    public static Object getFirstBlockData(DBPage dbPage, ColumnType colType) {
        return getBlockData(dbPage, FIRST_BLOCK_OFFSET, colType);
    }

    /**
     * 计算指定值的结束偏移量
     * 
     * @param dbPage 数据页
     * @param blockStart 起始位置
     * @param colType 列类型
     * @return 返回该值的结束位置
     */
    public static int getBlockEndOffset(DBPage dbPage, int blockStart, ColumnType colType) {

        PageReader uncReader = new PageReader(dbPage);

        uncReader.setPosition(ENCODING_OFFSET);

        if (uncReader.readInt() != ENCODING_MARKER) {
            throw new IllegalArgumentException("Wrong encoding type");
        }

        uncReader.setPosition(NEXT_BLOCK_START_OFFSET);

        if (uncReader.readInt() <= blockStart) {
            return -1;
        }

        return blockStart + DBPage.getObjectDiskSize(dbPage.readObject(blockStart, colType), colType) + 4;
    }

    /**
     * 第一个block的结束偏移量
     * 
     * @param dbPage 数据页
     * @param colType 列类型
     * @return 偏移量
     */
    public static int getFirstBlockEndOffset(DBPage dbPage, ColumnType colType) {
        return getBlockEndOffset(dbPage, FIRST_BLOCK_OFFSET, colType);
    }
}
