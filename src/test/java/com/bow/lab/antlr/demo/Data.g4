grammar Data;

file: group+;

group: INT sequence[$INT.int] {System.out.println($sequence.text);} ;

sequence[int n]
locals [int i=1;]
    :( {$i<=$n}? INT {$i++;} )*
    ;

INT : [0-9]+ ;
WS : [ \r\t\n]+ -> skip ;