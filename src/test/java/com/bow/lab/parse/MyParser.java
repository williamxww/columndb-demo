package com.bow.lab.parse;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATN;

/**
 * @author vv
 * @since 2017/10/9.
 */
public class MyParser extends Parser {
    public MyParser(TokenStream input) {
        super(input);
    }

    @Override
    public String[] getTokenNames() {
        return new String[0];
    }

    @Override
    public String[] getRuleNames() {
        return new String[0];
    }

    @Override
    public String getGrammarFileName() {
        return null;
    }

    @Override
    public ATN getATN() {
        return null;
    }
}
