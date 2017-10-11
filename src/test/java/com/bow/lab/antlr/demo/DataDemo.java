package com.bow.lab.antlr.demo;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * @author wwxiang
 * @since 2017/10/11.
 */
public class DataDemo {

    public static void main(String[] args) {
        String content = "2 9 10 3 1 2 3";
        DataParser parser = new DataParser(new CommonTokenStream(new DataLexer(new ANTLRInputStream(content))));
        parser.file();
    }
}
