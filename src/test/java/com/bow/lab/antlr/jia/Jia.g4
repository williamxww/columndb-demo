grammar Jia;
@header {
  /**
   * All rights reserved by vv.
   */
}
// 定义一个语法  data 操作A操作B操作C操作D data
// 10 +*- 2  等同于  ((10+2)*2)-2 ，值为 22
expression : expression (PLUS|SUB|MUL|DIV)* NUMBER| NUMBER;
NUMBER : [0]|[1-9]+[0-9]*;
PLUS : 'p';
SUB : 's' ;
MUL : 'm' ;
DIV : 'd' ;
WS : [ \t\n\r]+ -> skip ;