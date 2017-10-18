package com.bow.lab.parse;

import edu.caltech.nanodb.commands.FromClause;
import edu.caltech.nanodb.commands.SelectClause;
import edu.caltech.nanodb.commands.SelectCommand;
import edu.caltech.nanodb.commands.SelectValue;
import edu.caltech.nanodb.expressions.BooleanOperator;
import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.CompareOperator;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.expressions.OrderByExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Stack;

import static com.bow.lab.parse.SQLiteParser.*;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class DemoListener extends SQLiteBaseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoListener.class);

    private SelectClause sc;

    private Stack<Expression> exprStack = new Stack<>();

    public SelectCommand getResult() {
        SelectCommand cmd = new SelectCommand(sc);
        return cmd;
    }

    private void printStack() {
        StringBuilder sb = new StringBuilder();
        for (Expression e : exprStack) {
            sb.append(e).append("|");
        }
        LOGGER.trace(sb.toString());
    }

    /**
     * 解析获取对应的expr对象并入栈
     * 
     * @param ctx ctx
     */
    @Override
    public void enterExpr(SQLiteParser.ExprContext ctx) {
        // 若expr对应为column_name
        if (ctx.column_name() != null) {
            ColumnName cn = new ColumnName();
            cn.setColumnName(ctx.column_name().getText());

            if (ctx.table_name() != null) {
                cn.setTableName(ctx.table_name().getText());
            }

            ColumnValue cv = new ColumnValue(cn);
            exprStack.push(cv);
        }

        Literal_valueContext litCnx = ctx.literal_value();
        LiteralValue lv = null;
        if (litCnx != null) {
            if (litCnx.STRING_LITERAL() != null) {
                String str = litCnx.STRING_LITERAL().getText();
                lv = new LiteralValue(str);
            } else if (litCnx.NUMERIC_LITERAL() != null) {
                String str = litCnx.NUMERIC_LITERAL().getText();
                BigDecimal bd = new BigDecimal(str);
                lv = new LiteralValue(bd);
            } else if (litCnx.K_NULL() != null) {
                lv = new LiteralValue(null);
            }
            exprStack.push(lv);
        }
        printStack();
    }

    /**
     * 退出时进行合并计算
     * 
     * @param ctx ctx
     */
    @Override
    public void exitExpr(SQLiteParser.ExprContext ctx) {

        // 比较大小
        CompareOperator.Type cmpType = null;
        if (ctx.ASSIGN() != null) {
            cmpType = CompareOperator.Type.EQUALS;
        } else if (ctx.GT() != null) {
            cmpType = CompareOperator.Type.GREATER_THAN;
        } else if (ctx.GT_EQ() != null) {
            cmpType = CompareOperator.Type.GREATER_OR_EQUAL;
        } else if(ctx.LT() != null){
            cmpType = CompareOperator.Type.LESS_THAN;
        } else if(ctx.LT_EQ() != null){
            cmpType = CompareOperator.Type.LESS_OR_EQUAL;
        }

        if (cmpType != null) {
            Expression right = exprStack.pop();
            Expression left = exprStack.pop();
            CompareOperator result = new CompareOperator(cmpType, left, right);
            exprStack.push(result);
            printStack();
        }

        // 逻辑与或非
        BooleanOperator boolExpr = null;
        if (ctx.K_AND() != null) {
            boolExpr = new BooleanOperator(BooleanOperator.Type.AND_EXPR);
            Expression right = exprStack.pop();
            Expression left = exprStack.pop();
            boolExpr.addTerm(left);
            boolExpr.addTerm(right);
        } else if (ctx.K_OR() != null) {
            boolExpr = new BooleanOperator(BooleanOperator.Type.OR_EXPR);
            Expression right = exprStack.pop();
            Expression left = exprStack.pop();
            boolExpr.addTerm(left);
            boolExpr.addTerm(right);
        } else if (ctx.K_NOT() != null) {
            boolExpr = new BooleanOperator(BooleanOperator.Type.NOT_EXPR);
            boolExpr.addTerm(exprStack.pop());
        }

        if (boolExpr != null) {
            exprStack.push(boolExpr);
            printStack();
        }


    }

    @Override
    public void exitResult_column(Result_columnContext ctx) {

        Expression cv = exprStack.pop();
        String alias = null;
        if (ctx.K_AS() != null) {
            alias = ctx.column_alias().getText();
        }
        SelectValue sv = new SelectValue(cv, alias);
        sc.addSelectValue(sv);
        printStack();
    }

    @Override
    public void exitWhere_clause(SQLiteParser.Where_clauseContext ctx) {
        Expression where = exprStack.pop();
        sc.setWhereExpr(where);
        printStack();
    }

    @Override
    public void exitGroup_clause(SQLiteParser.Group_clauseContext ctx) {
        while (!exprStack.isEmpty()) {
            Expression group = exprStack.pop();
            sc.addGroupByExpr(group);
        }
        printStack();
    }

    @Override
    public void exitHaving_cluase(SQLiteParser.Having_cluaseContext ctx) {
        Expression having = exprStack.pop();
        sc.setHavingExpr(having);
        printStack();
    }

    @Override
    public void exitOrdering_term(SQLiteParser.Ordering_termContext ctx) {
        boolean ascending = false;
        if (ctx.K_ASC() != null) {
            ascending = true;
        }
        Expression e = exprStack.pop();
        OrderByExpression order = new OrderByExpression(e, ascending);
        sc.addOrderByExpr(order);
        printStack();
    }

    @Override
    public void enterSelect_core(Select_coreContext ctx) {
        this.sc = new SelectClause();

        String table = ctx.table_or_subquery().get(0).getText();
        FromClause fc = new FromClause(table, null);
        sc.setFromClause(fc);

        // distinct | all
        if (ctx.K_DISTINCT() != null) {
            sc.setDistinct(true);
        } else if (ctx.K_ALL() != null) {
            //
        }
    }

    @Override
    public void exitFactored_select_stmt(Factored_select_stmtContext ctx) {
        // stack 的最后2个肯定是limit 和 offset
        if (ctx.K_OFFSET() != null) {
            Expression offset = exprStack.pop();
            BigDecimal val = (BigDecimal) offset.evaluate(null);
            sc.setOffset(val.intValue());
        }
        if (ctx.K_LIMIT() != null) {
            Expression limit = exprStack.pop();
            BigDecimal val = (BigDecimal) limit.evaluate(null);
            sc.setLimit(val.intValue());
        }

    }

    @Override
    public void enterCreate_table_stmt(Create_table_stmtContext ctx) {
        System.out.println("create table " + ctx.table_name().getText());
        for (Column_defContext col : ctx.column_def()) {
            System.out.println(col.column_name().getText() + " " + col.type_name().getText());
        }
    }

    @Override
    public void enterDrop_table_stmt(Drop_table_stmtContext ctx) {
        System.out.println(ctx.table_name().getText());
    }

}
