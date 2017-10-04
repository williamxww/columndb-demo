package com.bow.lab.antlr.jia;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

/**
 * @author vv
 * @since 2017/10/4.
 */
public class JiaTest {

    /**
     * 10 +*- 2 等同于 ((10+2)*2)-2 ，值为 22<br/>
     * PLUS : 'p'; SUB : 's' ; MUL : 'm' ; DIV : 'd' ;
     */
    @Test
    public void listen() {
        String expr = "10 pms 2";
        CharStream inputStream = CharStreams.fromString(expr);
        JiaLexer lexer = new JiaLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        JiaParser parser = new JiaParser(tokenStream);
        ParseTreeWalker walker = new ParseTreeWalker();
        CustomJiaListener cl = new CustomJiaListener();
        walker.walk(cl, parser.expression());
        System.out.println(cl.result());
    }

    @Test
    public void visit() {
        String expr = "10 pms 2";
        CharStream inputStream = CharStreams.fromString(expr);
        JiaLexer lexer = new JiaLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        JiaParser parser = new JiaParser(tokenStream);
        CustomJiaVisitor visitor = new CustomJiaVisitor();
        // 使用visitor去访问语法解析器获取到的表达式的各个节点，visitor封装了算法
        Integer result = visitor.visit(parser.expression());
        System.out.println(result);
    }

}
