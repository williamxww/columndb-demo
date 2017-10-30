package edu.caltech.nanodb.plans;

import edu.caltech.nanodb.expressions.OrderByExpression;

import java.util.List;

/**
 * @author vv
 * @since 2017/10/30.
 */
public interface Marked {

    /**
     * This method reports whether this plan node supports marking a certain
     * point in the tuple-stream so that processing can return to that point as
     * needed.
     *
     * @return true if the node supports position marking, false otherwise.
     */
    public abstract boolean supportsMarking();

    /**
     * 要求左子节点支持marking This method reports whether this plan node requires the
     * left child to support marking for proper evaluation.
     *
     * @return true if the node requires that its left child supports marking,
     *         false otherwise.
     */
    public abstract boolean requiresLeftMarking();

    /**
     * 要求右子节点支持marking This method reports whether this plan node requires the
     * right child to support marking for proper evaluation.
     *
     * @return true if the node requires that its right child supports marking,
     *         false otherwise.
     */
    public abstract boolean requiresRightMarking();

    /**
     * Marks the current tuple in the tuple-stream produced by this node. The
     * {@link #resetToLastMark} method can be used to return to this tuple. Note
     * that only one marker can be set in the tuple-stream at a time.
     *
     * @throws UnsupportedOperationException if the node does not support
     *         marking.
     *
     * @throws IllegalStateException if there is no "current tuple" to mark.
     *         This will occur if {@link PlanNode#getNextTuple} hasn't yet been
     *         called (i.e. we are before the first tuple in the tuple-stream),
     *         or if we have already reached the end of the tuple-stream (i.e.
     *         we are after the last tuple in the stream).
     */
    public abstract void markCurrentPosition();

    /**
     * Resets the node's tuple-stream to the most recently marked position. Note
     * that only one marker can be set in the tuple-stream at a time.
     *
     * @throws UnsupportedOperationException if the node does not support
     *         marking.
     *
     * @throws IllegalStateException if {@link #markCurrentPosition} hasn't yet
     *         been called on this plan-node
     */
    public abstract void resetToLastMark();
}
