- 在IDEA中安装 antlr4插件


- 编写 Jia.g4

```g4
grammar Jia;
@header {
  /**
   * comments
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
LINE_COMMENT : '//' .*? '\n' -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
```

- 对 Jia.g4右键 Generate antr recognization 生成语法识别代码


- 编写监听器CustomJiaListener

```java
package com.bow.lab.antlr.jia;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 定义一个语法 data 操作A操作B操作C操作D data<br/>
 * 10 +*- 2 等同于 ((10+2)*2)-2 ，值为 22
 * 
 * @author vv
 * @since 2017/10/4.
 */
public class CustomJiaListener extends JiaBaseListener {

    private Stack<Integer> numStack = new Stack<>();

    private Queue<String> symbolQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void enterExpression(JiaParser.ExpressionContext ctx) {
        super.enterExpression(ctx);
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        super.enterEveryRule(ctx);
    }

    /**
     * 在每次访问节点时会触发此方法。
     * 
     * @param node 表达式经过语法解析器拆分成的各个节点。
     */
    @Override
    public void visitTerminal(TerminalNode node) {
        super.visitTerminal(node);
        String text = node.getText();
        if (isCalSymbol(text))
            symbolQueue.add(text);
        else
            cal(text);
    }

    private boolean isCalSymbol(String text) {
        return text.equals("p") || text.equals("s") || text.equals("m") || text.equals("d");
    }

    private void cal(String text) {
        if (text != null && !text.equals(" ")) {
            if (symbolQueue.isEmpty()) {
                numStack.push(Integer.parseInt(text));
            } else {
                while (!symbolQueue.isEmpty()) {
                    String symbol = symbolQueue.poll();
                    switch (symbol) {
                    case "p":
                        numStack.push(numStack.pop() + Integer.parseInt(text));
                        break;
                    case "s":
                        numStack.push(numStack.pop() - Integer.parseInt(text));
                        break;
                    case "m":
                        numStack.push(numStack.pop() * Integer.parseInt(text));
                        break;
                    case "l":
                        numStack.push(numStack.pop() / Integer.parseInt(text));
                        break;
                    default:
                        throw new RuntimeException("not support symbol");
                    }
                }
            }
        }
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        super.visitErrorNode(node);
    }

    /**
     * 最后调用此方法获取结果。
     * 
     * @return 最终结果
     */
    public int result() {
        return numStack.pop();
    }

}

```

- 或者编写visitor

```java
package com.bow.lab.antlr.jia;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author vv
 * @since 2017/10/4.
 */
public class CustomJiaVisitor extends JiaBaseVisitor<Integer> {
    private Stack<Integer> numStack = new Stack<>();

    private Queue<String> symbolQueue = new ConcurrentLinkedQueue<>();

    /**
     * 访问到节点后，如果是符号就存储到队列中，是数字就使其参与计算并将结果返回。
     * @param node
     * @return
     */
    @Override
    public Integer visitTerminal(TerminalNode node) {
        String text = node.getText();
        if (isCalSymbol(text))
            symbolQueue.add(text);
        else
            cal(text);
        return numStack.peek();
    }

    private boolean isCalSymbol(String text) {
        return text.equals("p") || text.equals("s") || text.equals("m") || text.equals("d");
    }

    private void cal(String text) {
        if (text != null && !text.equals(" ")) {
            if (symbolQueue.isEmpty()) {
                numStack.push(Integer.parseInt(text));
            } else {
                while (!symbolQueue.isEmpty()) {
                    String symbol = symbolQueue.poll();
                    switch (symbol) {
                    case "p":
                        numStack.push(numStack.pop() + Integer.parseInt(text));
                        break;
                    case "s":
                        numStack.push(numStack.pop() - Integer.parseInt(text));
                        break;
                    case "m":
                        numStack.push(numStack.pop() * Integer.parseInt(text));
                        break;
                    case "l":
                        numStack.push(numStack.pop() / Integer.parseInt(text));
                        break;
                    default:
                        throw new RuntimeException("not support symbol");
                    }
                }
            }
        }
    }
}

```



- 应用程序

```java
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

```



对着某个语法右键 Test rule expresstion可以即时生成对应的语法树

