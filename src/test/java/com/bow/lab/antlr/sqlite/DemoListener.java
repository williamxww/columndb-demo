package com.bow.lab.antlr.sqlite;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import javax.xml.bind.SchemaOutputResolver;
import java.util.HashSet;
import java.util.Set;

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
    }

    @Override
    public void enterExpr(SQLiteParser.ExprContext ctx) {
        System.out.println("enterExpr");
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
