/*
    public static void main(String[] args) throws Exception {
        String[] testStr = { "2", "a+b+3", "(a-b)+3", "a+(b*3)" };
        for (String s : testStr) {
            System.out.println("Input expr:" + s);
            DemoParser parser = new DemoParser(new CommonTokenStream(new DemoLexer(CharStreams.fromString(s))));
            parser.stat();
        }
    }

*/

grammar Demo;

@header {
  /**
   * All rights reserved by vv.
   */
//  package com.bow.lab.antlr.demo;
//
//  import java.util.ArrayList;
//  import java.util.List;
}

//parser
stat:expr|NEWLINE
;

expr:multExpr(('+'|'-')multExpr)*
;
multExpr:atom(('*'|'/')atom)*
;
atom:'('expr')'
    |INT
    |ID
;

//lexer
NEWLINE:'\r'?'\n';
LINE_COMMENT : '//' .*? '\n' -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r]+ -> skip ;

ID:('a'..'z'|'A'..'Z')+;
INT:'0'..'9'+;