
/*
  public static void main(String[] args) {
      String content = "10,2,5,8";
      CountParser parser = new CountParser(new CommonTokenStream(new CountLexer(new ANTLRInputStream(content))));
      parser.list();
  }
*/
grammar Count;
@header {
    //  package csv;
}

@members {
    int count = 0;
}

list

@after {
    System.out.println(count+" ints");
} : INT {count++;} (',' INT {count++;} )*
    ;

INT : [0-9]+ ;
WS : [ \r\t\n]+ -> skip ;