### 用例

```sql
CREATE TABLE states (
  id   INTEGER,
  name VARCHAR(30)
);
INSERT INTO states VALUES (7, 'A');
INSERT INTO states VALUES (8, 'B');
INSERT INTO states VALUES (9, 'C');
INSERT INTO states VALUES (10, 'D');
INSERT INTO states VALUES (11, 'E');
select * from dual where name =a order by age asc limit 10 offset 1;
```





### 专业术语

schema : column info 的集合
RLE:run-length encoding,变动长度编码法,游程编码是一种简单的非破坏性资料压缩法



LogSequenceNumber，lsn是联系dirty page，redo log record和redo log file的纽带。在每个redo log record被拷贝到内存的log buffer时会产生一个相关联的lsn，而每个页面修改时会产生一个log record，从而每个数据库的page也会有一个相关联的lsn，这个lsn记录在每个page的header字段中。为了保证WAL（Write-Ahead-Logging）要求的逻辑，dirty page要求其关联lsn的log record已经被写入log file才允许执行flush操作









```java
// pageData[position]为byte，若全为1，不加& 0xFF返回的是-1，但需要返回无符号整数
public int readUnsignedByte(int position) {
    return pageData[position] & 0xFF;
}
// 注意此处为a++
public long readUnsignedInt(int position) {
        long value = ((pageData[position++] & 0xFF) << 24)
                   | ((pageData[position++] & 0xFF) << 16)
                   | ((pageData[position++] & 0xFF) <<  8)
                   | ((pageData[position  ] & 0xFF)      );

        return value;
    }
```


```
at edu.caltech.nanodb.util.ArrayUtil.sizeOfDifferentRange(ArrayUtil.java:59)
at edu.caltech.nanodb.storage.writeahead.WALManager.writeUpdatePageRecord(WALManager.java:832)
at edu.caltech.nanodb.transactions.TransactionManager.recordPageUpdate(TransactionManager.java:271)
at edu.caltech.nanodb.storage.StorageManager.logDBPageWrite(StorageManager.java:460)
at edu.caltech.nanodb.storage.heapfile.HeapFileTableManager.addTuple(HeapFileTableManager.java:741)
at edu.caltech.nanodb.commands.InsertCommand.insertSingleRow(InsertCommand.java:209)
at edu.caltech.nanodb.commands.InsertCommand.execute(InsertCommand.java:165)
at edu.caltech.nanodb.server.NanoDBServer.doCommand(NanoDBServer.java:114)
at edu.caltech.nanodb.client.ExclusiveClient.main(ExclusiveClient.java:106)
```

### 文件存储

edu.caltech.nanodb.storage.StorageManager#createDBFile

createDBFile时，就写入了FileType和页大小encodePageSize



#### xxx.tbl

记录表结构的文件

headPage

```
// 存储整体结构数据
|    1B  |       1B     |   4B    |
|FileType|encodePageSize|chemaSize|
// 存储列信息
|  1B      |  1B  |   1B     |  XB   |  1B  |   1B     |  XB   |...
|numColumns|TypeID|colNameLen|colName|TypeID|colNameLen|colName|...
// 存储约束
|  1B          |  1B          |  1B          |  1B   |  1B    |
|numConstraints|ConstraintType|ConstraintName|keySize|ColIndex|
// 存储表统计信息
|NumDataPages|NumTuples|AvgTupleSize|
// 列统计信息
|nullMask|numUnique|numNull|colType|minVal|colType|maxVal|
```



第二页(pageNo:1)

DataPage开始2字节表示总slot数 ，后面每个slot占用一个short

numSlots:总槽位数

slotVal: 其中存放的是tuple的Offset

tuple的值是从page的末尾开始存放的，在存放tuple前会先放置nullFlag，每个bit代表此tuple在对应列是否为Null

edu.caltech.nanodb.storage.heapfile.HeapFilePageTuple#storeNewTuple

```
|  2 B   |  2 B  |  2 B  |...
|numSlots|slotVal|slotVal|...
.............

|nullFlag|tuple col1|tuple col2|
```





#### txnstat.dat

txnstat.dat 只记录某次事务在日志文件(.log文件)中的起始位置（firstLSN），下次写日志的起始位置（nextLSN）和下次事务的id
edu.caltech.nanodb.storage.writeahead.WALManager#doRecovery
firstLSN:		需要恢复时，从此处开始恢复
nextLSN:	需要写时，从此处接着写

LogSequenceNumber：logFileNo+offset，它指明了某次日志记录在文件中的起始位置。

```
|    1B  |       1B     |   4B      |      2B      |      4B      |      2B     |      4B     |
|FileType|encodePageSize|NEXT_TXN_ID|firstLSNFileNo|firstLSNOffset|nextLSNFileNo|nextLSNOffset|
```



#### wal-xxx.log

typeId:21

wal-00000.log: write ahead log

edu.caltech.nanodb.storage.writeahead.WALManager#openWALFile

undo:修改前的数据

redo:修改后的数据

prevFileEndOffset：前一个WAL结尾的offset

rec's fileOffset:当前记录的LSN，即此记录的开始offset

WALRecType：每条日志的末尾放type是便于回滚时根据type算出当前日志的起始位置。

```
|    1B  |       1B     |         4B      |
|FileType|encodePageSize|prevFileEndOffset|

1.START_TXN，start txn是没有preLSN的
|    1B    | 4B  |    1B    |
|WALRecType|txnId|WALRecType|

2.其他TxnRecord的格式
|    1B    | 4B  |      2B     |      4B     |    1B    |
|WALRecType|txnId|prevLSNFileNo|prevLSNOffset|WALRecType|

3.UPDATE_PAG
|    1B    | 4B  |      2B     |      4B     |    x B   |  2B  |    2B     |
|WALRecType|txnId|prevLSNFileNo|prevLSNOffset|DBFileName|PageNo|numSegments|
| 2B  | 2B |        xB       | 2B  | 2B |        xB       |...|        4B      |    1B    |
|index|size|undo or redo data|index|size|undo or redo data|...|rec's fileOffset|WALRecType|

4.UPDATE_PAGE_REDO_ONLY,与UPDATE_PAG相比没有了undo data
|    1B    | 4B  |      2B     |      4B     |    x B   |  2B  |    2B     |
|WALRecType|txnId|prevLSNFileNo|prevLSNOffset|DBFileName|PageNo|numSegments|
| 2B  | 2B |    xB   | 2B  | 2B |     xB   |...|        4B      |    1B    |
|index|size|redo data|index|size| redo data|...|rec's fileOffset|WALRecType|
```



#### 列式数据库结构

CSHeaderPage

```
// 存储整体结构数据
|    1B  |       1B     |   4B    |    4B    |  1B      |
|FileType|encodePageSize|   -1    |schemaSize|numColumns|
// 存储列信息
|  1B  |   1B     |  XB   |  1B  |   1B     |  XB   |...
|TypeID|colNameLen|colName|TypeID|colNameLen|colName|...
// 存储约束
|  1B          |  1B          |  1B          |  1B   |  1B    |
|numConstraints|ConstraintType|ConstraintName|keySize|ColIndex|
// 存储表统计信息
|NumDataPages|NumTuples|AvgTupleSize|
// 列统计信息
|nullMask|numUnique|numNull|colType|minVal|colType|maxVal|
```

#### 列数据结构(新)

记录表结构的文件

xxx.frm

```
// 存储整体结构数据
|    1B  |       1B     |   4B    |
|FileType|encodePageSize|chemaSize|
// 存储列信息
|  1B      |  1B  |   1B     |  XB   |  1B  |   1B     |  XB   |...
|numColumns|TypeID|colNameLen|colName|TypeID|colNameLen|colName|...
// 存储约束
|  1B          |  1B          |  1B          |  1B   |  1B    |
|numConstraints|ConstraintType|ConstraintName|keySize|ColIndex|
// 存储表统计信息
|NumDataPages|NumTuples|AvgTupleSize|
// 列统计信息
|nullMask|numUnique|numNull|colType|minVal|colType|maxVal|
```

xxx.col.data

对于定长字段如int

```
|FileType|encodePageSize|colType|beginOffset|nextOffset|
|numSlots|maxVal|minVal|
|slotVal|slotVal|...
```

变长字段如varchar

```
|FileType|encodePageSize|colType|beginOffset|nextOffset|
|numSlots|maxVal|minVal|
|length|data|length|data|...
```



#### UncompressedPage

文件压缩方式: 对于UncompressedPage，采用的是无压缩

count:每增加一条记录,count++

NEXT_BLOCK_OFFSET:下次写的位置

```
// 存储整体结构数据
|    1B  |       1B     |   4B      |  4B  |        4B       |
|FileType|encodePageSize|文件压缩方式|count |NEXT_BLOCK_OFFSET|

| 1B   |    4B      |  1B  |    4B     |
| 数据 | 数据偏移量  | 数据 | 数据偏移量  |
```





### 创建列式数据库

```sql
CREATE COLSTORE states FROM  vv.txt (
  id   INTEGER,
  name VARCHAR(30)
);
select name from states;
select name from states where id > 3 limit 2 offset 1;
select count(1) from states;
select concat(name, '1') from vv;
INSERT INTO states VALUES (1, 'a');
INSERT INTO states VALUES (2, 'b');
INSERT INTO states VALUES (3, 'c');

-- 之后改造成
CREATE TABLE states (
  id   INTEGER,
  name VARCHAR(30)
) engine=columnstore;

LOAD DATA INFILE 'states.txt'
INTO TABLE states
CHARACTER SET utf8
FIELDS TERMINATED BY ',';
```


一个库一个DBFile前2字节为
1 byte type | 1 byte encoded page-size |


表schema存储文件 vv.tbl
CSHeaderPage：用于存放各列的类型，名称以及相关主键。
DataFileType: 列式存储类型为30
encodePageSize:page size 用2的幂值表示，如65536的encodePageSize为16
colNums:总列数
colTypeId:列类型，如Int对应的1
colTypeLength:列宽度，只有当列类型为VARCHAR时，此值有效
colName:列名称
numConstraints:主外键的数量
keys:主外键
```
|    1B  |       1B     |   4B   |  4B      |
|FileType|encodePageSize|encoding|schemaSize|

|   1B  |    1B   |    2B       |
|colNums|colTypeId|colTypeLength|colName|numConstraints|keys|
```

列存储文件 vv.id.tbl
DataFileType: 列式存储类型为30
encodePageSize:page size 用2的幂值表示，如65536的encodePageSize为16
encoding: 是压缩存储还是非压缩
count: 此页总记录数
next_write_pos:写下一条记录时的起始位置

```
|    1B  |       1B     |   4B   |  4B |      4B   	  |
|FileType|encodePageSize|encoding|count|next_write_pos|

|  4B   |  4B  |
|int val|  pos |int val|pos|...

|      4B      |     X B   |  4B   |
|varchar length|varchar val|  pos  |varchar length|varchar val|  pos  |
```

### 执行计划

select t.id,t.name, s.id from test t inner join states s on t.id=s.id where t.id>0;

```

NestedLoopsJoinNode
	|
RenameNode
	|
FileScanNode
```



### 相关算法

Bloom Filter



### 启动日志

edu.caltech.nanodb.client.ExclusiveClient#main

```verilog
INFO  main NanoDBServer| Initializing storage manager.
INFO  main StorageManager| Using base directory .\datafiles
INFO  main StorageManager| Initializing transaction manager.
DEBUG main BufferManager| Requested file txnstate.dat is NOT in file-cache.
INFO  main TransactionManager| Couldn't find transaction-state file txnstate.dat, creating.
DEBUG main BufferManager| Requested file txnstate.dat is NOT in file-cache.
DEBUG main FileManager| Creating new database file .\datafiles\txnstate.dat.
DEBUG main FileManager| Initializing database file .\datafiles\txnstate.dat.
DEBUG main FileManager| Requested page 0 doesn't yet exist in file txnstate.dat; creating.
DEBUG main FileManager| Set file txnstate.dat length to 8192
DEBUG main BufferManager| Adding file txnstate.dat to file-cache.
DEBUG main BufferManager| Requested page [txnstate.dat,0] is NOT in page-cache.
DEBUG main BufferManager| Adding page [txnstate.dat,0] to page-cache.
DEBUG main BufferManager| Session 1 is pinning page [txnstate.dat,0].  New pin-count is 1.
INFO  main BufferManager| Writing all dirty pages for file txnstate.dat to disk (with sync).
DEBUG main BufferManager|     Saving page [txnstate.dat,0] to disk.
DEBUG main BufferManager| Syncing file txnstate.dat
INFO  main FileManager| Synchronizing database file to disk:  txnstate.dat
DEBUG main TransactionManager| Txn State has FirstLSN = LSN[00000:00000006], NextLSN = LSN[00000:00000006]
DEBUG main BufferManager| Requested file txnstate.dat is in file-cache.
DEBUG main BufferManager| Requested page [txnstate.dat,0] is in page-cache.
INFO  main BufferManager| Writing all dirty pages for file txnstate.dat to disk (with sync).
DEBUG main BufferManager|     Saving page [txnstate.dat,0] to disk.
DEBUG main BufferManager| Syncing file txnstate.dat
INFO  main FileManager| Synchronizing database file to disk:  txnstate.dat
Welcome to NanoDB.  Exit with EXIT or QUIT command.
```





### CREATE日志

```sql
CREATE TABLE states (
  id   INTEGER,
  name VARCHAR(30)
);
```



```
DEBUG main ExclusiveClient| Parsed command:  CreateTable[STATES]
DEBUG main TransactionStateUpdater| Session ID:  1	Transaction state:  TxnState[no transaction]
DEBUG main TransactionStateUpdater| No transaction is in progress; auto-starting one!
DEBUG main TransactionManager| Starting transaction with ID 1
DEBUG main CreateTableCommand| Creating a TableFileInfo object describing the new table STATES.
DEBUG main CreateTableCommand| Creating the new table STATES on disk.
DEBUG main FileManager| Creating new database file .\datafiles\STATES.tbl.
DEBUG main FileManager| Initializing database file .\datafiles\STATES.tbl.
DEBUG main FileManager| Requested page 0 doesn't yet exist in file STATES.tbl; creating.
DEBUG main FileManager| Set file STATES.tbl length to 8192
DEBUG main StorageManager| Created new DBFile for table STATES at path .\datafiles\STATES.tbl
INFO  main HeapFileTableManager| Initializing new table STATES with 2 columns, stored at STATES.tbl
DEBUG main BufferManager| Requested page [STATES.tbl,0] is NOT in page-cache.
DEBUG main BufferManager| Adding page [STATES.tbl,0] to page-cache.
DEBUG main BufferManager| Session 1 is pinning page [STATES.tbl,0].  New pin-count is 1.
INFO  main HeapFileTableManager| Writing table schema:  Schema[cols=[ColumnInfo[STATES.ID:INTEGER], ColumnInfo[STATES.NAME:VARCHAR(30)]]]
DEBUG main HeapFileTableManager| Writing 0 constraints
DEBUG main HeapFileTableManager| Constraints occupy 1 bytes in the schema
DEBUG main HeapFileTableManager| Table STATES schema uses 14 bytes of the 8192-byte header page.
DEBUG main HeaderPage| Writing table-statistics to header page.
DEBUG main HeaderPage| Writing column-stat data:  nullmask=0xF, unique=-1, null=-1, min=null, max=null
DEBUG main HeaderPage| Writing column-stat data:  nullmask=0xF, unique=-1, null=-1, min=null, max=null
DEBUG main HeaderPage| Writing statistics completed.  Total size is 12 bytes.
DEBUG main TransactionManager| Recording page-update for page 0 of file STATES.tbl
DEBUG main WALManager| Writing a START_TXN record for transaction 1 at LSN LSN[00000:00000006]
DEBUG main WALManager| Opening WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is NOT in file-cache.
DEBUG main WALManager| WAL file doesn't exist!  WAL is expanding into a new file.
DEBUG main WALManager| Creating WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is NOT in file-cache.
DEBUG main FileManager| Creating new database file .\datafiles\wal-00000.log.
DEBUG main FileManager| Initializing database file .\datafiles\wal-00000.log.
DEBUG main FileManager| Requested page 0 doesn't yet exist in file wal-00000.log; creating.
DEBUG main FileManager| Set file wal-00000.log length to 8192
DEBUG main BufferManager| Adding file wal-00000.log to file-cache.
DEBUG main BufferManager| Requested page [wal-00000.log,0] is NOT in page-cache.
DEBUG main BufferManager| Adding page [wal-00000.log,0] to page-cache.
DEBUG main BufferManager| Session 1 is pinning page [wal-00000.log,0].  New pin-count is 1.
DEBUG main WALManager| Next-LSN value is now LSN[00000:00000012]
DEBUG main WALManager| Writing an UPDATE_PAGE record for transaction 1 at LSN LSN[00000:00000012]
DEBUG main WALManager| Opening WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
DEBUG main BufferManager| Requested page [wal-00000.log,0] is in page-cache.
DEBUG main WALManager| Skipping identical bytes starting at index 0
DEBUG main WALManager| Recording changed bytes starting at index 3
DEBUG main WALManager| Found 16 changed bytes starting at index 3
DEBUG main WALManager| Skipping identical bytes starting at index 19
DEBUG main WALManager| Recording changed bytes starting at index 30
DEBUG main WALManager| Found 2 changed bytes starting at index 30
DEBUG main WALManager| Skipping identical bytes starting at index 32
DEBUG main BufferManager| Session 1 is unpinning page [STATES.tbl,0].  New pin-count is 0.
DEBUG main CreateTableCommand| New table STATES is created!
Created table:  STATES
DEBUG main TransactionStateUpdater| Session ID:  1	Transaction state:  TxnState[txnID=1, userStarted=false, loggedStart=true, lastLSN=LSN[00000:00000012]]
DEBUG main TransactionStateUpdater| An auto-started transaction is in progress; committing it!
DEBUG main WALManager| Writing a COMMIT_TXN record for transaction 1 at LSN LSN[00000:00000087]
DEBUG main WALManager| Opening WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
DEBUG main BufferManager| Requested page [wal-00000.log,0] is in page-cache.
DEBUG main WALManager| Next-LSN value is now LSN[00000:00000099]
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
INFO  main BufferManager| Writing all dirty pages for file wal-00000.log to disk (with sync).
DEBUG main BufferManager|     Saving page [wal-00000.log,0] to disk.
DEBUG main BufferManager| Syncing file wal-00000.log
INFO  main FileManager| Synchronizing database file to disk:  wal-00000.log
DEBUG main BufferManager| Requested file txnstate.dat is in file-cache.
DEBUG main BufferManager| Requested page [txnstate.dat,0] is in page-cache.
INFO  main BufferManager| Writing all dirty pages for file txnstate.dat to disk (with sync).
DEBUG main BufferManager|     Saving page [txnstate.dat,0] to disk.
DEBUG main BufferManager| Syncing file txnstate.dat
INFO  main FileManager| Synchronizing database file to disk:  txnstate.dat
DEBUG main TransactionManager| WAL was successfully forced to LSN LSN[00000:00000099] (plus 0 bytes)
DEBUG main TransactionManager| Transaction completed, resetting transaction state.
DEBUG main BufferManager| Session 1 is unpinning page [wal-00000.log,0].  New pin-count is 0.
DEBUG main BufferManager| Session 1 is unpinning page [txnstate.dat,0].  New pin-count is 0.
CMD> 
```



### INSERT日志

```sql
INSERT INTO states VALUES (1, 'Alabama');
INSERT INTO states VALUES (2, 'Alaska');
INSERT INTO states VALUES (3, 'Arizona');
INSERT INTO states VALUES (4, 'Arkansas');
INSERT INTO states VALUES (5, 'California');
INSERT INTO states VALUES (6, 'wuhan');
INSERT INTO states VALUES (7, '1');
INSERT INTO states VALUES (8, 'vv');
```



```log
INSERT INTO states VALUES (1, 'Alabama');
DEBUG main ExclusiveClient| Parsed command:  InsertCommand[values=(1,'Alabama')]
DEBUG main TransactionStateUpdater| Session ID:  1	Transaction state:  TxnState[no transaction]
DEBUG main TransactionStateUpdater| No transaction is in progress; auto-starting one!
DEBUG main TransactionManager| Starting transaction with ID 2
DEBUG main EventDispatcher| Firing beforeRowInserted
DEBUG main HeapFileTableManager| Adding new tuple of size 14 bytes.
DEBUG main BufferManager| Requested page [STATES.tbl,1] is NOT in page-cache.
DEBUG main HeapFileTableManager| Reached end of data file without finding space for new tuple.
DEBUG main HeapFileTableManager| Creating new page 1 to store new tuple.
DEBUG main BufferManager| Requested page [STATES.tbl,1] is NOT in page-cache.
DEBUG main FileManager| Requested page 1 doesn't yet exist in file STATES.tbl; creating.
DEBUG main FileManager| Set file STATES.tbl length to 16384
DEBUG main BufferManager| Adding page [STATES.tbl,1] to page-cache.
DEBUG main BufferManager| Session 1 is pinning page [STATES.tbl,1].  New pin-count is 1.
DEBUG main DataPage| Allocating space for new 14-byte tuple.
DEBUG main DataPage| Current number of slots on page:  0
DEBUG main DataPage| No empty slot available.  Adding a new slot.
DEBUG main DataPage| Tuple will get slot 0.  Final number of slots:  1
DEBUG main DataPage| New tuple of 14 bytes will reside at location [8178, 8192).
DEBUG main HeapFileTableManager| New tuple will reside on page 1, slot 0.
DEBUG main TransactionManager| Recording page-update for page 1 of file STATES.tbl
DEBUG main WALManager| Writing a START_TXN record for transaction 2 at LSN LSN[00000:00000099]
DEBUG main WALManager| Opening WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
DEBUG main BufferManager| Requested page [wal-00000.log,0] is in page-cache.
DEBUG main BufferManager| Session 1 is pinning page [wal-00000.log,0].  New pin-count is 1.
DEBUG main WALManager| Next-LSN value is now LSN[00000:00000105]
DEBUG main WALManager| Writing an UPDATE_PAGE record for transaction 2 at LSN LSN[00000:00000105]
DEBUG main WALManager| Opening WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
DEBUG main BufferManager| Requested page [wal-00000.log,0] is in page-cache.
DEBUG main WALManager| Skipping identical bytes starting at index 0
DEBUG main WALManager| Recording changed bytes starting at index 1
DEBUG main WALManager| Found 3 changed bytes starting at index 1
DEBUG main WALManager| Skipping identical bytes starting at index 4
DEBUG main WALManager| Recording changed bytes starting at index 8182
DEBUG main WALManager| Found 10 changed bytes starting at index 8182
DEBUG main EventDispatcher| Firing afterRowInserted
DEBUG main IndexUpdater| Adding tuple to indexes for table STATES
DEBUG main TransactionStateUpdater| Session ID:  1	Transaction state:  TxnState[txnID=2, userStarted=false, loggedStart=true, lastLSN=LSN[00000:00000105]]
DEBUG main TransactionStateUpdater| An auto-started transaction is in progress; committing it!
DEBUG main WALManager| Writing a COMMIT_TXN record for transaction 2 at LSN LSN[00000:00000170]
DEBUG main WALManager| Opening WAL file wal-00000.log
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
DEBUG main BufferManager| Requested page [wal-00000.log,0] is in page-cache.
DEBUG main WALManager| Next-LSN value is now LSN[00000:00000182]
DEBUG main BufferManager| Requested file wal-00000.log is in file-cache.
INFO  main BufferManager| Writing all dirty pages for file wal-00000.log to disk (with sync).
DEBUG main BufferManager|     Saving page [wal-00000.log,0] to disk.
DEBUG main BufferManager| Syncing file wal-00000.log
INFO  main FileManager| Synchronizing database file to disk:  wal-00000.log
DEBUG main BufferManager| Requested file txnstate.dat is in file-cache.
DEBUG main BufferManager| Requested page [txnstate.dat,0] is in page-cache.
DEBUG main BufferManager| Session 1 is pinning page [txnstate.dat,0].  New pin-count is 1.
INFO  main BufferManager| Writing all dirty pages for file txnstate.dat to disk (with sync).
DEBUG main BufferManager|     Saving page [txnstate.dat,0] to disk.
DEBUG main BufferManager| Syncing file txnstate.dat
INFO  main FileManager| Synchronizing database file to disk:  txnstate.dat
DEBUG main TransactionManager| WAL was successfully forced to LSN LSN[00000:00000182] (plus 0 bytes)
DEBUG main TransactionManager| Transaction completed, resetting transaction state.
DEBUG main BufferManager| Session 1 is unpinning page [wal-00000.log,0].  New pin-count is 0.
DEBUG main BufferManager| Session 1 is unpinning page [txnstate.dat,0].  New pin-count is 0.
DEBUG main BufferManager| Session 1 is unpinning page [STATES.tbl,1].  New pin-count is 0.
CMD> 
```



### 查询日志

select *  from states;



###  DROP TABLE

```
drop table SS;
DEBUG main ExclusiveClient| Parsed command:  DropTable[SS]
DEBUG main TransactionStateUpdater| Session ID:  1	Transaction state:  TxnState[no transaction]
DEBUG main TransactionStateUpdater| No transaction is in progress; auto-starting one!
DEBUG main TransactionManager| Starting transaction with ID 7
INFO  main BufferManager| Flushing all pages for file SS.tbl from the Buffer Manager.
DEBUG main BufferManager|     Evicting page [SS.tbl,0] from page-cache.
DEBUG main BufferManager|     Evicted page is dirty; must save to disk.
DEBUG main TransactionManager| Request to force WAL to LSN LSN[00000:00000360] unnecessary; already forced to LSN[00000:00000439].
INFO  main BufferManager| Removing DBFile SS.tbl from buffer manager
INFO  main BufferManager| Flushing all pages for file SS.tbl from the Buffer Manager.
INFO  main FileManager| Synchronizing database file to disk:  SS.tbl
INFO  main FileManager| Closing database file:  SS.tbl
DEBUG main TransactionStateUpdater| Session ID:  1	Transaction state:  TxnState[txnID=7, userStarted=false, loggedStart=false, lastLSN=null]
DEBUG main TransactionStateUpdater| An auto-started transaction is in progress; committing it!
DEBUG main TransactionManager| Transaction 7 has made no changes; not recording transaction-commit to WAL.
DEBUG main TransactionManager| Transaction completed, resetting transaction state.
CMD> 
```



p4
