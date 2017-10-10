package com.bow.lab.antlr.csv;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.List;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class CsvDemo {

    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("aaa,bbb,ccc\n");
        sb.append("aa,\"cc\",a");
        CommonTokenStream tokens = new CommonTokenStream(new CSVLexer(CharStreams.fromString(sb.toString())));
        CSVParser parser = new CSVParser(tokens);
        List<List<String>> data = parser.file().data;

        for(List<String> row: data){
            StringBuilder s = new StringBuilder();
            for(String t: row){
                s.append(t).append(" ");
            }
            System.out.println(s);
        }
    }

}
