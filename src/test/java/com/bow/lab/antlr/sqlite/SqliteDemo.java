package com.bow.lab.antlr.sqlite;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class SqliteDemo {

    public static void main(String[] args) throws Exception {
        String sql = "SELECT log AS x FROM t1 \n" + "GROUP BY x\n" + "HAVING count(*) >= 4 \n" + "ORDER BY max(n) + 0";

        String s = "select * from dual where name = 'a' order by name desc limit 5 offset 3";
        SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(s));
        SQLiteParser parser = new SQLiteParser(new CommonTokenStream(lexer));

        ParseTree tree = parser.select_stmt();
        ParseTree tree1 = parser.compound_select_stmt();

        ParseTreeWalker.DEFAULT.walk(new DemoListener(), tree);
    }
}
