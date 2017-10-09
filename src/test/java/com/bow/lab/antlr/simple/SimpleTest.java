package com.bow.lab.antlr.simple;

import com.bow.lab.antlr.sqlite.SQLiteLexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * @author vv
 * @since 2017/10/9.
 */
public class SimpleTest {
    public static void main(String[] args) {
        String content = "class Person{}";
        SimpleParser parser = new SimpleParser(new CommonTokenStream(new SQLiteLexer(new ANTLRInputStream(content))));
        parser.removeErrorListeners(); // remove ConsoleErrorListener
        parser.addErrorListener(new MyANTLRErrorListener()); // add ours
        parser.prog(); // parse as usual
    }
}
