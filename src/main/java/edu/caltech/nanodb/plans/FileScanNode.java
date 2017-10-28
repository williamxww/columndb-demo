package edu.caltech.nanodb.plans;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.OrderByExpression;
import edu.caltech.nanodb.qeval.PlanCost;
import edu.caltech.nanodb.qeval.SelectivityEstimator;
import edu.caltech.nanodb.qeval.TableStats;
import edu.caltech.nanodb.relations.Tuple;
import edu.caltech.nanodb.storage.TableFileInfo;
import edu.caltech.nanodb.storage.TableManager;

/**
 * A select plan-node that scans a table file, checking the optional predicate
 * against each tuple in the file.
 */
public class FileScanNode extends SelectNode {

    private static Logger logger = Logger.getLogger(FileScanNode.class);

    public TableFileInfo tblFileInfo;

    /**
     * 便于后面通过{@link #advanceCurrentTuple()}跳到此tuple上，通过
     * {@link #markCurrentPosition()}设置
     */
    private Tuple markedTuple;

    /**
     * 通过{@link #advanceCurrentTuple()}推进的时候是否回到指定位置，通过
     * {@link #resetToLastMark()}设置
     */
    private boolean jumpToMarkedTuple;

    public FileScanNode(TableFileInfo tblFileInfo, Expression predicate) {
        super(predicate);

        if (tblFileInfo == null)
            throw new NullPointerException("table cannot be null");

        this.tblFileInfo = tblFileInfo;
    }

    /**
     * Currently we will always say that the file-scan node produces unsorted
     * results. In actuality, a file scan's results will be sorted if the table
     * file uses a sequential format, but currently we don't have any sequential
     * file formats.
     */
    public List<OrderByExpression> resultsOrderedBy() {
        return null;
    }

    protected void prepareSchema() {
        // Grab the schema from the table.
        schema = tblFileInfo.getSchema();
    }

    /**
     * 获取schema stats cost
     */
    @Override
    public void prepare() {
        // 获取schema和表统计信息
        schema = tblFileInfo.getSchema();
        TableStats tableStats = tblFileInfo.getStats();
        stats = tableStats.getAllColumnStats();

        // 没有谓词时选择率为100%，否则就要评估了
        float selectivity = 1.0f;
        if (predicate != null) {
            selectivity = SelectivityEstimator.estimateSelectivity(predicate, schema, stats);
        }

        // 计算cost
        float numTuples = tableStats.numTuples;
        numTuples *= selectivity;
        // The CPU cost是根据总tuple数计算的
        cost = new PlanCost(numTuples, tableStats.avgTupleSize, tableStats.numTuples, tableStats.numDataPages);

        // TODO: We should update the table statistics based on the predicate,
        // but for now we'll leave them unchanged.
    }

    @Override
    public void initialize() {
        super.initialize();
        // 重置marked
        markedTuple = null;
        jumpToMarkedTuple = false;
    }

    public void cleanUp() {
        // Nothing to do!
    }

    /**
     * 向前推进currentTuple,即获取下一个tuple然后赋值给currentTuple
     *
     * @throws java.io.IOException if the TableManager failed to open the table.
     */
    @Override
    protected void advanceCurrentTuple() throws IOException {

        if (jumpToMarkedTuple) {
            // 如果标记了，则跳到marked位置
            logger.debug("Resuming at previously marked tuple.");
            currentTuple = markedTuple;
            jumpToMarkedTuple = false;
            return;
        }

        TableManager tableManager = tblFileInfo.getTableManager();
        if (currentTuple == null) {
            // currentTuple 若为null则getFirstTuple
            currentTuple = tableManager.getFirstTuple(tblFileInfo);
        } else {
            currentTuple = tableManager.getNextTuple(tblFileInfo, currentTuple);
        }
    }

    /**
     * 标记当前位置，方便回退
     */
    @Override
    public void markCurrentPosition() {
        if (currentTuple == null)
            throw new IllegalStateException("There is no current tuple!");

        logger.debug("Marking current position in tuple-stream.");
        markedTuple = currentTuple;
    }

    /**
     * 回退
     */
    @Override
    public void resetToLastMark() {
        if (markedTuple == null)
            throw new IllegalStateException("There is no last-marked tuple!");

        logger.debug("Resetting to previously marked position in tuple-stream.");
        // 下次advanceCurrentTuple时跳到指定位置
        jumpToMarkedTuple = true;
    }

    @Override
    public boolean supportsMarking() {
        return true;
    }

    @Override
    public boolean requiresLeftMarking() {
        return false;
    }

    @Override
    public boolean requiresRightMarking() {
        return false;
    }

    /**
     * Returns true if the passed-in object is a <tt>FileScanNode</tt> with the
     * same predicate and table.
     *
     * @param obj the object to check for equality
     *
     * @return true if the passed-in object is equal to this object; false
     *         otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileScanNode) {
            FileScanNode other = (FileScanNode) obj;
            return tblFileInfo.equals(other.tblFileInfo) && predicate.equals(other.predicate);
        }
        return false;
    }

    /**
     * Computes the hashcode of a PlanNode. This method is used to see if two
     * plan nodes CAN be equal.
     **/
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (predicate != null ? predicate.hashCode() : 0);
        hash = 31 * hash + tblFileInfo.hashCode();
        return hash;
    }

    /**
     * Creates a copy of this simple filter node node and its subtree. This
     * method is used by {@link PlanNode#duplicate} to copy a plan tree.
     */
    @Override
    protected PlanNode clone() throws CloneNotSupportedException {
        FileScanNode node = (FileScanNode) super.clone();
        // The table-info doesn't need to be copied since it's immutable.
        node.tblFileInfo = tblFileInfo;
        return node;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("FileScan[");
        buf.append("table:  ").append(tblFileInfo.getTableName());
        if (predicate != null)
            buf.append(", pred:  ").append(predicate.toString());
        buf.append("]");
        return buf.toString();
    }
}
