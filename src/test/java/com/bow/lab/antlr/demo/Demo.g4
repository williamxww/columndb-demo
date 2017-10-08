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
// WS:(' '|'\t'|'\n'|'\r')+{skip();};
LINE_COMMENT : '//' .*? '\n' -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r]+ -> skip ;

ID:('a'..'z'|'A'..'Z')+;
INT:'0'..'9'+;