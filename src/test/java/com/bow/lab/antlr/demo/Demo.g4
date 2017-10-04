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
prog:stat
;
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
ID:('a'..'z'|'A'..'Z')+;
INT:'0'..'9'+;
NEWLINE:'\r'?'\n';
WS:(' '|'\t'|'\n'|'\r')+{skip();};