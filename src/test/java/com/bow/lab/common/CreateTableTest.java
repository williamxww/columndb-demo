package com.bow.lab.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.caltech.nanodb.commands.DropTableCommand;
import edu.caltech.nanodb.commands.InsertCommand;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.server.EventDispatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.caltech.nanodb.commands.CreateTableCommand;
import edu.caltech.nanodb.commands.FromClause;
import edu.caltech.nanodb.commands.LoadFileCommand;
import edu.caltech.nanodb.commands.SelectClause;
import edu.caltech.nanodb.commands.SelectCommand;
import edu.caltech.nanodb.commands.SelectValue;
import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.SQLDataType;
import edu.caltech.nanodb.storage.StorageManager;

/**
 * @author vv
 * @since 2017/10/22.
 */
public class CreateTableTest {

    private String tableName = "TEST";

    @Before
    public void setup() throws IOException {
        StorageManager.init();
    }

    @After
    public void destroy() throws IOException {
        StorageManager.shutdown();
    }

    @Test
    public void create() throws Exception {

        CreateTableCommand command = new CreateTableCommand(tableName, false, false);
        ColumnType strType = new ColumnType(SQLDataType.VARCHAR);
        ColumnType intType = new ColumnType(SQLDataType.INTEGER);
        strType.setLength(10);
        ColumnInfo id = new ColumnInfo("ID", tableName, intType);
        ColumnInfo name = new ColumnInfo("NAME", tableName, strType);
        command.addColumn(id);
        command.addColumn(name);

        // 开启事务并执行命令
        EventDispatcher eventDispatch = EventDispatcher.getInstance();
        eventDispatch.fireBeforeCommandExecuted(command);
        command.execute();
        eventDispatch.fireAfterCommandExecuted(command);
    }

    @Test
    public void insert()throws Exception {
        List<String> names = new ArrayList<>();
        names.add("ID");
        names.add("NAME");
        List<Expression> expressions = new ArrayList<>();
        LiteralValue id = new LiteralValue("1");
        LiteralValue name = new LiteralValue("g");
        expressions.add(id);
        expressions.add(name);
        InsertCommand command = new InsertCommand(tableName, names, expressions);

        // 开启事务并执行命令
        EventDispatcher eventDispatch = EventDispatcher.getInstance();
        eventDispatch.fireBeforeCommandExecuted(command);
        command.execute();
        eventDispatch.fireAfterCommandExecuted(command);
    }

    @Test
    public void select() throws Exception {
        ColumnName idCn = new ColumnName("id");
        Expression idCv = new ColumnValue(idCn);
        SelectValue idVal = new SelectValue(idCv, null);
        SelectClause clause = new SelectClause();
        clause.addSelectValue(idVal);
        FromClause from = new FromClause(tableName, null);
        clause.setFromClause(from);
        SelectCommand command = new SelectCommand(clause);
        command.execute();
    }

    @Test
    public void drop() throws Exception {
        DropTableCommand command = new DropTableCommand(tableName,true);
        command.execute();
    }
}
