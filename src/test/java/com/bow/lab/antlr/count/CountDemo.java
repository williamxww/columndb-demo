package com.bow.lab.antlr.count;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * 统计整数的个数
 *
 * @author vv
 * @since 2017/10/10.
 */
public class CountDemo {

    public static void main(String[] args) {
        String content = "10,2,5,8";
        CountParser parser = new CountParser(new CommonTokenStream(new CountLexer(new ANTLRInputStream(content))));
        parser.list();
    }
}
