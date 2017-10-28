package edu.caltech.nanodb.plans;

import java.io.IOException;

import edu.caltech.nanodb.relations.Tuple;

import edu.caltech.nanodb.expressions.Expression;

/**
 * PlanNode representing the <tt>WHERE</tt> clause in a <tt>SELECT</tt>
 * operation. This is the relational algebra Select operator.
 */
public abstract class SelectNode extends PlanNode {

    /**
     * 用于判断此tuple是否符合要求的谓词
     */
    public Expression predicate;

    /**
     * 当前正在参与计算的tuple
     */
    protected Tuple currentTuple;

    /**
     * 扫描结束
     */
    private boolean done;

    /**
     * Constructs a SelectNode that scans a file for tuples.
     *
     * @param predicate the selection criterion
     */
    protected SelectNode(Expression predicate) {
        super(OperationType.SELECT);
        this.predicate = predicate;
    }

    protected SelectNode(PlanNode leftChild, Expression predicate) {
        super(OperationType.SELECT, leftChild);
        this.predicate = predicate;
    }

    @Override
    public void initialize() {
        super.initialize();

        done = false;
        currentTuple = null;
    }

    /**
     * 获取满足谓词的的tuple
     *
     * @return the tuple to be passed up to the next node.
     *
     * @throws java.lang.IllegalStateException if this is a scanning node with
     *         no algorithm or a filtering node with no child, or if the
     *         leftChild threw an IllegalStateException.
     *
     * @throws java.io.IOException if a db file failed to open at some point
     */
    @Override
    public Tuple getNextTuple() throws IllegalStateException, IOException {
        if (done) {
            return null;
        }

        // 遍历tuple直至找到满足谓词的tuple
        do {
            advanceCurrentTuple();
            if (currentTuple == null) {
                done = true;
                return null;
            }
        } while (!isTupleSelected(currentTuple));

        // The current tuple now satisfies the predicate, so return it.
        return currentTuple;
    }

    private boolean isTupleSelected(Tuple tuple) {
        // 谓词为null，则所有tuple都满足
        if (predicate == null) {
            return true;
        }
        // Set up the environment and then evaluate the predicate!
        environment.clear();
        environment.addTuple(schema, tuple);
        return predicate.evaluatePredicate(environment);
    }

    /**
     * Helper function that advances the current tuple reference in the node.
     *
     * @throws java.lang.IllegalStateException if this is a node with no
     *         algorithm or a filtering node with no child.
     * @throws java.io.IOException if a db file failed to open at some point
     */
    protected abstract void advanceCurrentTuple() throws IllegalStateException, IOException;

    /**
     * Creates a copy of this select node and its subtree. This method is used
     * by {@link PlanNode#duplicate} to copy a plan tree.
     */
    @Override
    protected PlanNode clone() throws CloneNotSupportedException {
        SelectNode node = (SelectNode) super.clone();

        // Copy the predicate.
        if (predicate != null)
            node.predicate = predicate.duplicate();
        else
            node.predicate = null;

        return node;
    }
}
