package com.bow.lab.antlr.sqlite;


/**
 * @author vv
 * @since 2017/10/8.
 */
public class DemoListener extends SQLiteBaseListener {

    @Override
    public void enterSelect_stmt(SQLiteParser.Select_stmtContext ctx) {
        if(ctx.K_LIMIT() != null){
            System.out.println("limit "+ctx.expr().get(0).getText());
        }
        if(ctx.K_OFFSET() != null){
            System.out.println("offset "+ ctx.expr().get(1).getText());
        }
    }

    @Override
    public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
        System.out.println("create table "+ctx.table_name().getText());
        for(SQLiteParser.Column_defContext col: ctx.column_def()){
            System.out.println(col.column_name().getText()+" "+col.type_name().getText());
        }
    }


    @Override
    public void enterDrop_table_stmt(SQLiteParser.Drop_table_stmtContext ctx) {
        System.out.println(ctx.table_name().getText());
    }


}
