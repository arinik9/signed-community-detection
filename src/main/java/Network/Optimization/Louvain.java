package Network.Optimization;

import Network.Core.Graph;
import Network.Core.ListMatrix;

import static Network.Core.ListMatrix.ROW;

/**
 * Louvain algorithm that partitions the network based on greedy node re-assignments
 * and hierarchical partition folding
 */
abstract public class Louvain implements Runnable{

    private Graph graph;
    private int[] partition;
    private int foldCount; // number of times graph is folded into partition for hierarchical detection
    private int id; // to be identified among other runnable-s

    public Louvain(){

    }

    public Louvain init(Graph graph, int[] initialPartition, int foldCount){
        this.graph = graph;
        this.partition = initialPartition;
        this.foldCount = foldCount;
        return this;
    }

    @Override
    public void run() {
        this.partition = detect(graph, partition, foldCount);
    }

    /**
     * Perform detect optimization on the graph with the given initial partition
     * @return
     */
    public int[] detect(Graph graph, int[] initialPartition, int foldCount){
        /**
         * A node n could be an aggregation of multiple nodes folded into on super node
         * pNC: positive weight from node n to group c, where group(n) = c
         * pCN: positive weight from group c to node n, where group(n) = c
         * pNCp : positive weight from node n to group c, where group(n) <> cp
         * pCpN : positive weight from group cp to node n, where group(n) <> cp
         * pin : positive in-weight of node n
         * pout : positive out-weight of node n
         * pself : positive self-loop node n
         * n*** : negative weight ...
         */
        int[] partition = initialPartition.clone();
        Graph transpose = (Graph) graph.transpose(true);
        float improvement = greedy(graph, transpose, partition);
        if(improvement <= 0.0 || foldCount == 0.0){
            // No further improvement was made by coarse-grain
            // or no further coarse-grain is needed
            return partition;
        }
        // Rebuild the network of communities:
        // Fold negative & positive sub-graphs separately according to partition
        Graph foldedGraph = graph.fold(partition);
        // At least 1% decrease in network size is expected
        double sizeRatio = (double) foldedGraph.getNodeCount() / graph.getNodeCount();
        if(sizeRatio > 0.99){
            return partition;
        }
        // Recursive detect optimization, partition the network of groups
        int[] superPartition = detect(foldedGraph,
                getInitialPartition(foldedGraph.getNodeCount()), foldCount - 1);
        /**
         * Node with groupId = g in the current level gets groupId = map[g] after coarsening
         * e.g. a node x is in group 10, group 10 is node 0 in folded graph
         * node 0 gets super-group 4, so node x is in group 4
         */
        int[] superGroup = new int[graph.getNodeCount()];
        ListMatrix foldedMatrix = foldedGraph.getListMatrix();
        int[] superNodeToGroup = foldedMatrix.getToRaw()[ROW];
        for(int superNodeId = 0 ; superNodeId < superPartition.length ; superNodeId++){
            // groupId of foldedGroup before being folded-normalized into a superNode
            int groupId = superNodeToGroup[superNodeId];
            superGroup[groupId] = superPartition[superNodeId];
        }
        // Change group id of node x with the corresponding superGroup of group id
        for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
            partition[nodeId] = superGroup[partition[nodeId]];
        }
        return partition; // detected partition
    }

    /**
     * Greedy optimization per folding that is used in detect,
     * Greedily moves nodes into best neighbor communities until convergence
     * @param graph
     * @param transpose transpose of graph for faster traverse on columns
     * @param partition this is the initial partition, changes are applied on this
     * @return improvement in objective function
     */
    abstract protected float greedy(Graph graph, Graph transpose, int[] partition);

    /**
     * Local change in the objective function by moving nodes between groups
     * @param parameters
     * @return
     */
    protected float localChange(ObjectiveParameters parameters){
        return 0;
    }

    /**
     * Returns an initial partition with each node in a separate group
     * @param size
     * @return
     */
    public static int[] getInitialPartition(int size){
        int[] partition = new int[size];
        for(int p = 0 ; p < partition.length ; p++){
            partition[p] = p;
        }
        return partition;
    }

    public int[] getPartition() {
        return partition;
    }

    public Louvain setId(int id) {
        this.id = id;
        return this;
    }

    public int getId() {
        return id;
    }
}