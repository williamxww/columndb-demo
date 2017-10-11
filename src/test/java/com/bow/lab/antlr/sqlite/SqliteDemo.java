package com.bow.lab.antlr.sqlite;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class SqliteDemo {

    protected SQLiteParser parse(String sql) {
        return new SQLiteParser(new CommonTokenStream(new SQLiteLexer(new ANTLRInputStream(sql))));
    }

    protected String getRuleName(ParserRuleContext ctx, int childIndex) {
        RuleContext ruleContext = (RuleContext) ctx.getChild(childIndex).getPayload();
        return SQLiteParser.ruleNames[ruleContext.getRuleIndex()];
    }

    protected void dumpTree(RuleContext ctx) {

        String lispTree = ctx.toStringTree(Arrays.asList(SQLiteParser.ruleNames));
        int indentation = 0;
        int index = 0;

        for (char ch : lispTree.toCharArray()) {
            if (ch == '(' & index > 0) {
                indentation++;
                System.out.print("\n");
                for (int i = 0; i < indentation; i++) {
                    System.out.print("  ");
                }
            } else if (ch == ')') {
                indentation--;
            }
            System.out.print(ch);
            index++;
        }

        System.out.println();
    }

    @Test
    public void common() {
        String sql = "SELECT log AS x FROM t1 \n" + "GROUP BY x\n" + "HAVING count(*) >= 4 \n" + "ORDER BY max(n) + 0";
        String select = "select * from dual where name = 'a' order by name desc limit 5 offset 3";
        String drop = "drop table vv";
        String create = "create table vv( name varchar, age int)";
        SQLiteParser parser = parse(create);
        ParseTree tree = parser.parse();
        ParseTreeWalker.DEFAULT.walk(new DemoListener(), tree);
    }

    @Test
    public void select() {
        String select = "select * from dual where name = 'a' order by name desc limit 5 offset 3";
        SQLiteParser parser = parse(select);
        ParseTree tree = parser.select_stmt();
        ParseTreeWalker.DEFAULT.walk(new DemoListener(), tree);
    }

    @Test
    public void colDef() {
        SQLiteParser parser = parse("_ID TEXT NOT NULL PRIMARY KEY");
        // Expected 'NOT NULL' and 'PRIMARY KEY' to be parsed as
        // column_constraints:
        // (column_def
        // (column_name _ID )
        // (type_name TEXT )
        // (column_constraint NOT NULL )
        // (column_constraint PRIMARY KEY ))
        SQLiteParser.Column_defContext ctx = parser.column_def();
        assertThat(ctx.getChildCount(), is(4));
        assertThat(getRuleName(ctx, 0), is("column_name"));
        assertThat(getRuleName(ctx, 1), is("type_name"));
        assertThat(getRuleName(ctx, 2), is("column_constraint"));
        assertThat(getRuleName(ctx, 3), is("column_constraint"));
    }

    @Test
    public void error(){
        String s = "select * from dual where name = 'a' order by name desc limit 5 offset 3";
        MyErrorListener errorListener = new MyErrorListener();
        MyParseListener parseListener = new MyParseListener();
        SQLiteParser parser = parse(s);
        parser.addErrorListener(errorListener);
        parser.addParseListener(parseListener);
        ParseTree tree = parser.select_stmt();
        ParseTreeWalker.DEFAULT.walk(new DemoListener(), tree);
    }

}
