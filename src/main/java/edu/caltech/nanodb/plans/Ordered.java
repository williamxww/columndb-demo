package edu.caltech.nanodb.plans;

import edu.caltech.nanodb.expressions.OrderByExpression;

import java.util.List;

/**
 * @author vv
 * @since 2017/10/30.
 */
public interface Ordered {
    /**
     * If the results are ordered in some way, this method returns a collection
     * of expressions specifying what columns or expressions the results are
     * ordered by. If the results are not ordered then this method may return
     * either an empty list or a <tt>null</tt> value.
     * <p>
     * When this method returns a list of ordering expressions, the order of the
     * expressions themselves also matters. The entire set of results will be
     * ordered by the first expression; rows with the same value for the first
     * expression will be ordered by the second expression; etc.
     *
     * @return If the plan node produces ordered results, this will be a list of
     *         objects specifying the ordering. If the node doesn't produce
     *         ordered results then the return-value will either be an empty
     *         list or it will be <tt>null</tt>.
     */
    public abstract List<OrderByExpression> resultsOrderedBy();
}
