grammar Vql;
@header {
  /**
   * Copyright (c) 2005-2011 by the California Institute of Technology.
   * All rights reserved.
   */
//  package edu.caltech.nanodb.sqlparse;

  import java.util.ArrayList;
  import java.util.List;

  import edu.caltech.nanodb.commands.*;
  import edu.caltech.nanodb.expressions.*;
  import edu.caltech.nanodb.relations.*;
}

tokens {
INT_LITERAL,
LONG_LITERAL,
FLOAT_LITERAL,
DEC_LITERAL,
PERIOD
}


command returns [Command c] @init{ $c = null; } :
  ( stmt1=drop_table_stmt|
    stmt2=exit_stmt
  ){ $c = $stmt1.c; }
  ;



exit_stmt returns [Command c] @init{ $c = null; } :
  ( EXIT | QUIT ) { $c = new ExitCommand(); } ;

drop_table_stmt returns [Command c]
  @init{
    $c = null;
    String name = null;
    boolean ifExists = false;
  }
  :
  DROP TABLE name=dbobj_ident
  { $c = new DropTableCommand($name.s, ifExists); }
  ;

dbobj_ident returns [String s] @init{ $s = null; } :
    IDENT { $s = $IDENT.text; } ;




NEWLINE:'\r'?'\n';
WS : [ \r\t\n]+ -> skip ;

// keywords
EXIT        : E X I T;
QUIT        : Q U I T;
DROP        : D R O P;
TABLE       : T A B L E;

// any string
IDENT : [a-zA-Z_] [a-zA-Z_0-9]*;


fragment DIGIT : [0-9];

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];