package com.bow.lab.antlr.sqlite;

/**
 * @author vv
 * @since 2017/10/9.
 */
public class DemoVisitor<T> extends SQLiteBaseVisitor<T> {


    @Override
    public T visitCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
        return visitChildren(ctx);
    }

}
