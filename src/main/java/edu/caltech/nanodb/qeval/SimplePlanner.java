package edu.caltech.nanodb.qeval;

import java.io.IOException;

import java.util.List;

import edu.caltech.nanodb.expressions.BooleanOperator;
import edu.caltech.nanodb.expressions.OrderByExpression;

import edu.caltech.nanodb.commands.FromClause;
import edu.caltech.nanodb.commands.SelectClause;
import edu.caltech.nanodb.commands.SelectValue;

import edu.caltech.nanodb.expressions.Expression;

import edu.caltech.nanodb.plans.FileScanNode;
import edu.caltech.nanodb.plans.NestedLoopsJoinNode;
import edu.caltech.nanodb.plans.PlanNode;
import edu.caltech.nanodb.plans.ProjectNode;
import edu.caltech.nanodb.plans.RenameNode;
import edu.caltech.nanodb.plans.SelectNode;
import edu.caltech.nanodb.plans.SimpleFilterNode;
import edu.caltech.nanodb.plans.SortNode;

import edu.caltech.nanodb.storage.StorageManager;
import edu.caltech.nanodb.storage.TableFileInfo;

import org.apache.log4j.Logger;

/**
 * This class generates execution plans for performing SQL queries. The primary
 * responsibility is to generate plans for SQL <tt>SELECT</tt> statements, but
 * <tt>UPDATE</tt> and <tt>DELETE</tt> expressions can also
 */
public class SimplePlanner implements Planner {

    private static Logger logger = Logger.getLogger(SimplePlanner.class);

    /**
     * 构建一个简单的执行计划
     * @param selClause select命令
     *
     * @return 执行计划
     * @throws IOException 在扫描文件时可能会有异常
     */
    public PlanNode makePlan(SelectClause selClause) throws IOException {

        FromClause fromClause = selClause.getFromClause();
        if (fromClause == null) {
            throw new UnsupportedOperationException("NanoDB doesn't yet support SQL queries without a FROM clause!");
        }

        // 构造JOIN或是子查询的执行计划
        PlanNode plan = makeJoinTree(fromClause);

        // 取出过滤的谓词融入到已有的plan中
        Expression whereExpr = selClause.getWhereExpr();
        if (whereExpr != null) {
            DPJoinPlanner.addPredicateToPlan(plan, whereExpr);
        }

        // TODO: Grouping/aggregation will go somewhere in here.

        // Depending on the SELECT clause, create a project node at the top of
        // the tree.
        if (!selClause.isTrivialProject()) {
            List<SelectValue> selectValues = selClause.getSelectValues();
            plan = new ProjectNode(plan, selectValues);
        }

        // Finally, apply any sorting at the end.
        List<OrderByExpression> orderByExprs = selClause.getOrderByExprs();
        if (!orderByExprs.isEmpty())
            plan = new SortNode(plan, orderByExprs);

        plan.prepare();

        return plan;
    }

    /**
     * Constructs a plan tree for evaluating the specified from-clause.
     * Depending on the clause's {@link FromClause#getClauseType type}, the plan
     * tree will comprise varying operations, such as:
     * <ul>
     * <li>{@link edu.caltech.nanodb.commands.FromClause.ClauseType#BASE_TABLE}
     * - the clause is a simple table reference, so a simple select operation is
     * constructed via {@link #makeSimpleSelect}.</li>
     * <li>
     * {@link edu.caltech.nanodb.commands.FromClause.ClauseType#SELECT_SUBQUERY}
     * - the clause is a <tt>SELECT</tt> subquery, so a plan subtree is
     * constructed by a recursive call to {@link #makePlan}.</li>
     * <li>{@link edu.caltech.nanodb.commands.FromClause.ClauseType#JOIN_EXPR} -
     * the clause is a join of two relations, so a join operation is created
     * between the left and right children of the from-clause. Plans for
     * generating the child results are constructed by recursive calls to
     * <tt>makeJoinTree()</tt>.</li>
     * </ul>
     *
     * @param fromClause the select nodes that need to be joined.
     *
     * @return a plan tree for evaluating the specified from-clause
     *
     * @throws IOException if an IO error occurs when the planner attempts to
     *         load schema and indexing information.
     */
    public PlanNode makeJoinTree(FromClause fromClause) throws IOException {

        PlanNode plan;

        FromClause.ClauseType clauseType = fromClause.getClauseType();
        switch (clauseType) {
        case BASE_TABLE:
        case SELECT_SUBQUERY:

            if (clauseType == FromClause.ClauseType.SELECT_SUBQUERY) {
                // 子查询，则为其生成执行计划
                plan = makePlan(fromClause.getSelectClause());
            } else {
                // 基表， 生成 file-scan plan node
                plan = makeSimpleSelect(fromClause.getTableName(), null);
            }

            // 有别名时，需要重命名该表
            if (fromClause.isRenamed())
                plan = new RenameNode(plan, fromClause.getResultName());

            break;

        case JOIN_EXPR:
            PlanNode leftChild = makeJoinTree(fromClause.getLeftChild());
            PlanNode rightChild = makeJoinTree(fromClause.getRightChild());

            Expression joinPredicate = fromClause.getPreparedJoinExpr();

            plan = new NestedLoopsJoinNode(leftChild, rightChild, fromClause.getJoinType(), joinPredicate);

            // If it's a NATURAL join, or a join with a USING clause, project
            // out the duplicate column names.
            List<SelectValue> selectValues = fromClause.getPreparedSelectValues();

            if (selectValues != null)
                plan = new ProjectNode(plan, selectValues);

            break;

        default:
            throw new IllegalArgumentException("Unrecognized from-clause type:  " + fromClause.getClauseType());
        }

        return plan;
    }

    /**
     * Constructs a simple select plan that reads directly from a table, with an
     * optional predicate for selecting rows.
     * <p>
     * While this method can be used for building up larger <tt>SELECT</tt>
     * queries, the returned plan is also suitable for use in <tt>UPDATE</tt>
     * and <tt>DELETE</tt> command evaluation. In these cases, the plan must
     * only generate tuples of type {@link edu.caltech.nanodb.storage.PageTuple}
     * , so that the command can modify or delete the actual tuple in the file's
     * page data.
     *
     * @param tableName The name of the table that is being selected from.
     *
     * @param predicate An optional selection predicate, or <tt>null</tt> if no
     *        filtering is desired.
     *
     * @return A new plan-node for evaluating the select operation.
     *
     * @throws IOException if an error occurs when loading necessary table
     *         information.
     */
    public SelectNode makeSimpleSelect(String tableName, Expression predicate) throws IOException {
        TableFileInfo tableInfo = StorageManager.getInstance().openTable(tableName);
        SelectNode node = new FileScanNode(tableInfo, predicate);
        return node;
    }
}
