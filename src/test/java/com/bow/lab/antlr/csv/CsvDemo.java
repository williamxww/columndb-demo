package com.bow.lab.antlr.csv;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.List;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class CsvDemo {

    public static void main(String[] args) throws Exception {
        // the input source
        String str = "aaa,bbb,ccc" + "\n" + "\"d,\"\"d\",eee,fff";

        // create an instance of the lexer
        CharStream in = CharStreams.fromString(str);
        CSVLexer lexer = new CSVLexer(in);

        // wrap a token-stream around the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // create the parser
        CSVParser parser = new CSVParser(tokens);

        // invoke the entry point of our grammar
        List<List<String>> data = parser.file().data;

        // display the contents of the CSV source
        for (int r = 0; r < data.size(); r++) {
            List<String> row = data.get(r);
            for (int c = 0; c < row.size(); c++) {
                System.out.println("(row=" + (r + 1) + ",col=" + (c + 1) + ") = " + row.get(c));
            }
        }
    }

}
