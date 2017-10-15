package com.bow.lab.parse;

import java.io.IOException;
import java.util.Scanner;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.log4j.Logger;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.util.StringUtil;

/**
 * This class is used for starting the NanoDB database in exclusive mode, where
 * only a single client interacts directly with the database system.
 */
public class Demo {

    private static Logger logger = Logger.getLogger(Demo.class);

    public static final String CMD_PROMPT = "CMD> ";

    public static void main(String args[]) {

        try {
            NanoDBServer.startup();
        } catch (IOException e) {
            System.out.println("DATABASE STARTUP FAILED:");
            e.printStackTrace(System.out);
            System.exit(1);
        }

        System.out.println("Welcome to NanoDB.  Exit with EXIT or QUIT command.\n");

        // 获取命令并执行
        Scanner scanner = new Scanner(System.in);
        System.out.print(CMD_PROMPT);
        while (scanner.hasNext()) {
            try {
                String line = scanner.nextLine();
                if (!StringUtil.isBlank(line)) {
                    line = line.trim();
                    // 退出
                    if ("exit".equals(line) || "bye".equals(line)) {
                        break;
                    }

                    // 词法&语法解析
                    SQLiteLexer lexer = new SQLiteLexer(CharStreams.fromString(line));
                    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                    SQLiteParser parser = new SQLiteParser(tokenStream);
                    ParseTree tree = parser.parse();
                    ParseListener listener = new ParseListener();
                    ParseTreeWalker.DEFAULT.walk(listener, tree);
                    Command cmd = listener.getResult();
                    logger.debug("Parsed command:  " + cmd);
                    NanoDBServer.doCommand(cmd, false);
                }
            } catch (Exception e) {
                logger.error("Unexpected error", e);
            }

            System.out.print(CMD_PROMPT);
        }

        // Shut down the various database subsystems that require cleanup.
        if (!NanoDBServer.shutdown()) {
            System.out.println("DATABASE SHUTDOWN FAILED.");
            System.exit(2);
        }
    }
}
