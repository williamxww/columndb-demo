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
