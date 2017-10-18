package com.bow.lab.parse;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * @author vv
 * @since 2017/10/9.
 */
public class MyParseListener implements ParseTreeListener {
    @Override
    public void visitTerminal(TerminalNode node) {
        System.out.println(node.getText());
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        System.out.println(node.getText());
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
    }
}
