package com.bow.lab.antlr.nano;

import edu.caltech.nanodb.commands.Command;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author wwxiang
 * @since 2017/10/11.
 */
public class VqlDemo {


    public static void main(String[] args) throws IOException {
        String content = "EXIT";
        Scanner scanner = new Scanner(System.in);
        content =scanner.nextLine();
        VqlParser parser = new VqlParser(new CommonTokenStream(new VqlLexer(CharStreams.fromString(content))));
        Command cmd = parser.command().c;
        System.out.println(cmd);
    }
}
