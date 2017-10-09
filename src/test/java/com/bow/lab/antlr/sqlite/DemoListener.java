package com.bow.lab.antlr.sqlite;

import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class DemoListener extends SQLiteBaseListener {

    @Override
    public void enterSelect_core(SQLiteParser.Select_coreContext ctx) {
        System.out.println("enterSelect_core");
    }

    @Override
    public void exitSelect_core(SQLiteParser.Select_coreContext ctx) {
        System.out.println("exitSelect_core");
    }

    @Override
    public void enterCompound_select_stmt(SQLiteParser.Compound_select_stmtContext ctx) {
        System.out.println("enterCompound_select_stmt");
        TerminalNode node = ctx.K_LIMIT();
        System.out.println(node);
    }

    @Override
    public void enterExpr(SQLiteParser.ExprContext ctx) {
        System.out.println("enterExpr");
        if (ctx.function_name() != null) {
            System.out.println(ctx.function_name().getText());
        }else if(ctx.select_stmt() != null){
            System.out.println(ctx.select_stmt().getText());
        }
    }

    @Override
    public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
        System.out.println("enterCreate_table_stmt");
    }

    @Override
    public void exitCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
        System.out.println("exitCreate_table_stmt");
    }

}
