package heuristics.geneticAlgorithm;

import java.util.*;

public class ProjectPreprocessor {
    private final Project project;
    
    // Preprocessing results
    public final int[] topologicalOrder;
    public final BitSet[] predClosure;
    public final int[] ESTprec;
    public final int[] nPredTotal;
    
    public ProjectPreprocessor(Project project) {
        this.project = project;
        this.topologicalOrder = computeTopologicalOrder();
        this.predClosure = computeTransitivePredecessorBitsets();
        this.ESTprec = computeEarliestStartTimes();
        this.nPredTotal = computeTotalPredecessorCounts();
    }
    
    /**
     * Compute topological order using Kahn's algorithm
     */
    private int[] computeTopologicalOrder() {
        int[] inDegree = new int[project.J + 2];
        Queue<Integer> queue = new LinkedList<>();
        List<Integer> result = new ArrayList<>();
        
        // Calculate in-degrees
        for (int j = 0; j <= project.J + 1; j++) {
            inDegree[j] = project.P[j].size();
        }
        
        // Add nodes with no incoming edges
        for (int j = 0; j <= project.J + 1; j++) {
            if (inDegree[j] == 0) {
                queue.offer(j);
            }
        }
        
        // Process nodes in topological order
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result.add(current);
            
            // Process all successors
            for (int i = 0; i < project.S[current].size(); i++) {
                int successor = project.S[current].get(i);
                inDegree[successor]--;
                if (inDegree[successor] == 0) {
                    queue.offer(successor);
                }
            }
        }
        
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Compute transitive closure of predecessors using DFS (deep first search)
     */
    private BitSet[] computeTransitivePredecessorBitsets() {
        BitSet[] closure = new BitSet[project.J + 2];
        boolean[] visited = new boolean[project.J + 2];
        
        for (int j = 0; j <= project.J + 1; j++) {
            closure[j] = new BitSet(project.J + 2);
        }
        
        // Process in reverse topological order for efficiency
        for (int idx = topologicalOrder.length - 1; idx >= 0; idx--) {
            int j = topologicalOrder[idx];
            if (!visited[j]) {
                computeTransitiveClosureDFS(j, closure, visited);
            }
        }
        
        return closure;
    }
    
    private void computeTransitiveClosureDFS(int j, BitSet[] closure, boolean[] visited) {
        visited[j] = true;
        
        // Add direct predecessors
        for (int i = 0; i < project.P[j].size(); i++) {
            int pred = project.P[j].get(i);
            closure[j].set(pred);
            
            // Recursively compute for predecessor if not yet done
            if (!visited[pred]) {
                computeTransitiveClosureDFS(pred, closure, visited);
            }
            
            // Add transitive predecessors
            closure[j].or(closure[pred]);
        }
    }
    
    /**
     * Compute earliest start times based on precedence only (no resources)
     */
    private int[] computeEarliestStartTimes() {
        int[] EST = new int[project.J + 2];
        
        // Process in topological order
        for (int j : topologicalOrder) {
            EST[j] = 0;
            
            // EST[j] = max_{i∈P[j]} (EST[i] + p[i])
            for (int i = 0; i < project.P[j].size(); i++) {
                int pred = project.P[j].get(i);
                EST[j] = Math.max(EST[j], EST[pred] + project.p[pred]);
            }
        }
        
        return EST;
    }
    
    /**
     * Compute total number of predecessors for each job
     */
    private int[] computeTotalPredecessorCounts() {
        int[] counts = new int[project.J + 2];
        
        for (int j = 0; j <= project.J + 1; j++) {
            counts[j] = predClosure[j].cardinality();
        }
        
        return counts;
    }
    
    /**
     * Create working copy of predecessor counts for SSGS
     */
    public int[] createPredecessorCountsForSSGS() {
        return nPredTotal.clone();
    }
    
    /**
     * Check if job a precedes job b in O(1)
     */
    public boolean precedes(int a, int b) {
        return predClosure[b].get(a);
    }
    
    /**
     * Get all jobs that must precede job j
     */
    public BitSet getAllPredecessors(int j) {
        return (BitSet) predClosure[j].clone();
    }
    
    /**
     * Check if a job list is precedence-feasible
     */
    public boolean isPrecedenceFeasible(int[] activityList) {
        Set<Integer> scheduled = new HashSet<>();
        
        for (int j : activityList) {
            // Check if all predecessors are already scheduled
            for (int i = 0; i < project.P[j].size(); i++) {
                int pred = project.P[j].get(i);
                if (!scheduled.contains(pred)) {
                    return false;
                }
            }
            scheduled.add(j);
        }
        
        return true;
    }
}
