package edu.caltech.nanodb.relations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.caltech.nanodb.expressions.ColumnName;

/**
 * <p>
 * A schema is an ordered collection of column names and associated types.
 * </p>
 * <p>
 * Many different entities in the database code can have schema associated with
 * them. Both tables and tuples have schemas, for obvious reasons.
 * <tt>SELECT</tt> and <tt>FROM</tt> clauses also have schemas, used by the
 * database engine to verify the semantics of database queries. Finally,
 * relational algebra plan nodes also have schemas, which specify the kinds of
 * tuples that they generate.
 * </p>
 */
public class Schema implements Serializable, Iterable<ColumnInfo> {

    /**
     * 为了方便通过表名和列名能够快速定位到该列，并且还要记住此列在schema中的顺序
     */
    private static class IndexedColumnInfo implements Serializable {
        /**
         * 第几列
         */
        public int colIndex;

        public ColumnInfo colInfo;

        public IndexedColumnInfo(int colIndex, ColumnInfo colInfo) {
            if (colInfo == null)
                throw new NullPointerException("colInfo cannot be null");

            if (colIndex < 0) {
                throw new IllegalArgumentException("colIndex must be >= 0; got " + colIndex);
            }

            this.colIndex = colIndex;
            this.colInfo = colInfo;
        }
    }

    /**
     * all columnInfo in schema.
     */
    private ArrayList<ColumnInfo> columnInfos;

    /**
     * Map<tableName, Map<columnName, columnInfo>><br/>
     * A mapping that provides fast lookups for columns based on table name and
     * column name. The outer hash-map has table names for keys; "no table" is
     * indicated with a <code>null</code> key, which {@link java.util.HashMap}
     * supports. The inner hash-map has column names for keys, and maps to
     * column information objects.
     */
    private Map<String, Map<String, IndexedColumnInfo>> tableColumn2InfoMap;

    /**
     * Map<columnName, ArrayList
     * <IndexedColumnInfo>>,同一个columnName可能对应多个ColumnInfo<br/>
     * 因为多个表连接时就会出现不同表有列名称是一样的情况<br/>
     * A mapping that provides fast lookups for columns based only on column
     * name. Because multiple columns could have the same column name (but
     * different table names) in a single schema, the values in the mapping are
     * lists.
     */
    private Map<String, ArrayList<IndexedColumnInfo>> name2InfosMap;

    public Schema() {
        columnInfos = new ArrayList<ColumnInfo>();
        tableColumn2InfoMap = new HashMap();
        name2InfosMap = new HashMap();
    }

    public Schema(List<ColumnInfo> colInfos) throws SchemaNameException {
        this();
        append(colInfos);
    }

    /**
     * Construct a copy of the specified schema object.
     *
     * @param s the schema object to copy
     */
    public Schema(Schema s) {
        this();
        append(s);
    }

    /**
     * Returns the number of columns in the schema.
     *
     * @return the number of columns in the schema.
     */
    public int numColumns() {
        return columnInfos.size();
    }

    /**
     * Returns the <tt>ColumnInfo</tt> object describing the column at the
     * specified index. Column indexes are numbered from 0.
     *
     * @param i the index to retrieve the column-info for
     *
     * @return the <tt>ColumnInfo</tt> object describing the name and type of
     *         the column
     */
    public ColumnInfo getColumnInfo(int i) {
        return columnInfos.get(i);
    }

    public List<ColumnInfo> getColumnInfos() {
        return Collections.unmodifiableList(columnInfos);
    }

    public Iterator<ColumnInfo> iterator() {
        return Collections.unmodifiableList(columnInfos).iterator();
    }

    /**
     * 添加列
     * 
     * @param colInfo 列信息
     * @return 增加列的序号
     */
    public int addColumnInfo(ColumnInfo colInfo) {
        if (colInfo == null)
            throw new NullPointerException("colInfo cannot be null");

        String colName = colInfo.getName();
        String tblName = colInfo.getTableName();

        // 表中若已有该元素，报错
        Map<String, IndexedColumnInfo> colMap = tableColumn2InfoMap.get(tblName);
        if (colMap != null && colMap.containsKey(colName)) {
            throw new SchemaNameException("Specified column " + colInfo + " is a duplicate of an existing column.");
        }

        // 获取即将添加的元素index
        int colIndex = columnInfos.size();
        columnInfos.add(colInfo);
        IndexedColumnInfo indexedColInfo = new IndexedColumnInfo(colIndex, colInfo);

        // 更新两个方便查询的map
        if (colMap == null) {
            colMap = new HashMap<String, IndexedColumnInfo>();
            tableColumn2InfoMap.put(tblName, colMap);
        }
        colMap.put(colName, indexedColInfo);

        ArrayList<IndexedColumnInfo> colList = name2InfosMap.get(colName);
        if (colList == null) {
            colList = new ArrayList<IndexedColumnInfo>();
            name2InfosMap.put(colName, colList);
        }
        colList.add(indexedColInfo);

        return colIndex;
    }

    /**
     * 将另一个Schema合并进来
     *
     * @throws SchemaNameException if any of the input column-info objects
     *         overlap the names of columns already in the schema.
     */
    public void append(Schema s) throws SchemaNameException {
        for (ColumnInfo colInfo : s) {
            addColumnInfo(colInfo);
        }
    }

    /**
     * Append a list of column-info objects to this schema.
     *
     * @throws SchemaNameException if multiple of the input column-info objects
     *         have duplicate column names, or overlap the names of columns
     *         already in the schema.
     */
    public void append(Collection<ColumnInfo> colInfos) throws SchemaNameException {
        for (ColumnInfo colInfo : colInfos)
            addColumnInfo(colInfo);
    }

    /**
     * Returns a set containing all table names that appear in this schema. Note
     * that this may include <code>null</code> if there are columns with no
     * table name specified!
     */
    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(tableColumn2InfoMap.keySet());
    }

    /**
     * This helper method returns the names of all tables that appear in both
     * this schema and the specified schema. Note that not all columns of a
     * given table must be present for the table to be included in the result;
     * there just has to be at least one column from the table in both schemas
     * for it to be included in the result.
     *
     * @param s the other schema to compare this schema to
     * @return a set containing the names of all tables that appear in both
     *         schemas
     */
    public Set<String> getCommonTableNames(Schema s) {
        HashSet<String> shared = new HashSet<String>(tableColumn2InfoMap.keySet());
        shared.retainAll(s.getTableNames());
        return shared;
    }

    /**
     * Returns a set containing all column names that appear in this schema.
     * Note that a column-name may be used by multiple columns, if it is
     * associated with multiple table names in this schema.
     *
     * @return a set containing all column names that appear in this schema.
     */
    public Set<String> getColumnNames() {
        return Collections.unmodifiableSet(name2InfosMap.keySet());
    }

    /**
     * 将当前schema中的列和参数中Schema的列名称合并后返回，注意会去重，主要是为了解决自然连接（不能有重复列） Returns the
     * names of columns that are common between this schema and the specified
     * schema. This kind of operation is mainly used for resolving
     * <tt>NATURAL</tt> joins.
     *
     * @param s the schema to compare to this schema
     * @return a set of the common column names
     */
    public Set<String> getCommonColumnNames(Schema s) {
        HashSet<String> shared = new HashSet<String>(name2InfosMap.keySet());
        shared.retainAll(s.getColumnNames());
        return shared;
    }

    /**
     * 指定名称colName在此schema中有多少个同样的列。 Returns the number of columns that have the
     * specified column name. Note that multiple columns can have the same
     * column name but different table names.
     *
     * @param colName the column name to return the count for
     * @return the number of columns with the specified column name
     */
    public int numColumnsWithName(String colName) {
        ArrayList<IndexedColumnInfo> list = name2InfosMap.get(colName);
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    /**
     * 自然连接是不允许有重复列(不同表的名称相同的列)的 This helper method returns true if this schema
     * contains any columns with the same column name but different table names.
     * If so, the schema is not valid for use on one side of a <tt>NATURAL</tt>
     * join.
     *
     * @return true if the schema has multiple columns with the same column name
     *         but different table names, or false otherwise.
     */
    public boolean hasMultipleColumnsWithSameName() {
        for (String cName : name2InfosMap.keySet()) {
            if (name2InfosMap.get(cName).size() > 1)
                return true;
        }
        return false;
    }

    /**
     * 根据列名获取列信息，如果一个colName有多个ColumnInfo会报错
     * 
     * @param colName the name of the column to retrieve the information for
     *
     * @return the column information for the specified column
     *
     * @throws SchemaNameException if the specified column name doesn't appear
     *         in this schema, or if it appears multiple times
     */
    public ColumnInfo getColumnInfo(String colName) {
        ArrayList<IndexedColumnInfo> list = name2InfosMap.get(colName);
        if (list == null || list.size() == 0)
            throw new SchemaNameException("No columns with name " + colName);

        if (list.size() > 1)
            throw new SchemaNameException("Multiple columns with name " + colName);

        return list.get(0).colInfo;
    }

    /**
     * 将此schema中所有列的表名都换成参数指定的tableName<br/>
     * This method iterates through all columns in this schema and sets them all
     * to be on the specified table. This method will throw an exception if the
     * result would be an invalid schema with duplicate column names.
     *
     * @throws SchemaNameException if the schema contains columns with the same
     *         column name but different table names. In this case, resetting
     *         the table name will produce an invalid schema with ambiguous
     *         column names.
     *
     * @design (donnie) At present, this method does this by replacing each
     *         {@link edu.caltech.nanodb.relations.ColumnInfo} object with a new
     *         object with updated information. This is because
     *         <code>ColumnInfo</code> is currently immutable.
     */
    public void setTableName(String tableName) throws SchemaNameException {

        // 检查：如果一个列名有2列的情况，就报错(覆盖失败)
        ArrayList<String> duplicateNames = null;
        for (Map.Entry<String, ArrayList<IndexedColumnInfo>> entry : name2InfosMap.entrySet()) {

            if (entry.getValue().size() > 1) {
                // 如果一个列名对应多个列，将此列名记录下来，做为错误信息抛出。
                if (duplicateNames == null) {
                    duplicateNames = new ArrayList<String>();
                }
                duplicateNames.add(entry.getKey());
            }
        }
        if (duplicateNames != null) {
            throw new SchemaNameException(
                    "Overriding table-name to " + tableName + " would produce ambiguous columns:  " + duplicateNames);
        }

        // 将所有列的tableName替换掉
        ArrayList<ColumnInfo> oldColInfos = columnInfos;
        // 将所有容器清空
        columnInfos = new ArrayList<ColumnInfo>();
        tableColumn2InfoMap = new HashMap();
        name2InfosMap = new HashMap();

        for (ColumnInfo colInfo : oldColInfos) {
            ColumnInfo newColInfo = new ColumnInfo(colInfo.getName(), tableName, colInfo.getType());
            // 添加修改后的ColumnInfo
            addColumnInfo(newColInfo);
        }
    }

    public int getColumnIndex(ColumnName colName) {
        if (colName.isColumnWildcard())
            throw new IllegalArgumentException("colName cannot be a wildcard");

        return getColumnIndex(colName.getTableName(), colName.getColumnName());
    }

    public int getColumnIndex(ColumnInfo colInfo) {
        return getColumnIndex(colInfo.getTableName(), colInfo.getName());
    }

    public int getColumnIndex(String colName) {
        return getColumnIndex(null, colName);
    }

    /**
     * 根据列明获取列序号
     * 
     * @param tblName 表名，只要此schema中不存在重复列明，不指定tblName也行
     * @param colName 列名
     * @return 列顺序号
     */
    public int getColumnIndex(String tblName, String colName) {
        ArrayList<IndexedColumnInfo> colList = name2InfosMap.get(colName);
        if (colList == null)
            return -1;

        if (tblName == null) {
            if (colList.size() > 1) {
                throw new SchemaNameException("Column name \"" + colName + "\" is ambiguous in this schema.");
            }
            return colList.get(0).colIndex;
        } else {
            // 指定了表名
            for (IndexedColumnInfo c : colList) {
                if (tblName.equals(c.colInfo.getTableName())) {
                    return c.colIndex;
                }
            }
        }
        return -1;
    }

    /**
     * 返回指定列明对应的列信息
     * 
     * @param colName 列明
     * @return Map<index, ColumnInfo>
     */
    public SortedMap<Integer, ColumnInfo> findColumns(ColumnName colName) {

        TreeMap<Integer, ColumnInfo> found = new TreeMap<Integer, ColumnInfo>();

        if (colName.isColumnWildcard()) {
            // Some kind of wildcard column-name object.

            if (!colName.isTableSpecified()) {
                // select * 将所有列返回
                for (int i = 0; i < columnInfos.size(); i++)
                    found.put(i, columnInfos.get(i));
            } else {
                // select tbl.* 将此table对应的列返回
                Map<String, IndexedColumnInfo> tableCols = tableColumn2InfoMap.get(colName.getTableName());
                if (tableCols != null) {
                    for (IndexedColumnInfo indexedColInfo : tableCols.values())
                        found.put(indexedColInfo.colIndex, indexedColInfo.colInfo);
                }
            }
        } else {

            // A non-wildcard column-name object.
            if (!colName.isTableSpecified()) {
                // 没有指定表名，则将此colName对应的所有列返回
                ArrayList<IndexedColumnInfo> colList = name2InfosMap.get(colName.getColumnName());
                if (colList != null) {
                    for (IndexedColumnInfo indexedColInfo : colList)
                        found.put(indexedColInfo.colIndex, indexedColInfo.colInfo);
                }
            } else {
                // select tbl.col ，将指定表对应的指定列返回
                Map<String, IndexedColumnInfo> tableCols = tableColumn2InfoMap.get(colName.getTableName());
                if (tableCols != null) {
                    IndexedColumnInfo indexedColInfo = tableCols.get(colName.getColumnName());
                    if (indexedColInfo != null)
                        found.put(indexedColInfo.colIndex, indexedColInfo.colInfo);
                }
            }
        }

        return found;
    }

    public String toString() {
        return "Schema[cols=" + columnInfos.toString() + "]";
    }
}
