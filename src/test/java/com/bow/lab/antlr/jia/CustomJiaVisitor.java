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
