/* 2 后面跟着2个数字，3后面跟着3个数字
    public static void main(String[] args) {
        String content = "2 9 10 3 1 2 3";
        DataParser parser = new DataParser(new CommonTokenStream(new DataLexer(new ANTLRInputStream(content))));
        parser.file();
    }
*/
grammar Data;

file: group+;

group: INT sequence[$INT.int] {System.out.println($sequence.text);} ;

sequence[int n]
locals [int i=1;]
    :( {$i<=$n}? INT {$i++;} )*
    ;

INT : [0-9]+ ;
WS : [ \r\t\n]+ -> skip ;