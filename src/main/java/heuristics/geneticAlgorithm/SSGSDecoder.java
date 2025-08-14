package heuristics.geneticAlgorithm;

public class SSGSDecoder {
    private final Project project;
    private final ProjectPreprocessor preprocessor;
    private final DecoderState state;
    
    // Performance optimization: track next resource release times
    private final int[] nextResourceRelease;
    
    public SSGSDecoder(Project project) {
        this.project = project;
        this.preprocessor = project.getPreprocessor();
        this.state = new DecoderState(project);
        this.nextResourceRelease = new int[project.K];
    }
    
    /**
     * Decode individual according to exact specification
     */
    public Individual decode(Individual ind, Project P) {
        // Allocate/clear DecoderState
        state.reset(P);
        
        // Schedule dummy 0 at t=0
        state.start[0] = 0;
        state.finish[0] = P.p[0]; // Usually 0 for dummy start
        
        // Iterate ind.activityList and place each job with SSGS
        for (int j : ind.activityList) {
            scheduleActivity(j);
        }
        
        // Finally place dummy J+1 at max finish
        scheduleActivity(P.J + 1);
        
        // Set ind.makespan = start[J+1]
        ind.makespan = state.start[P.J + 1];
        ind.start = state.start.clone();
        
        return ind;
    }
    
    /**
     * Decode activity list to schedule using SSGS (original method)
     */
    public Individual decode(int[] activityList) {
        Individual ind = new Individual(activityList);
        return decode(ind, project);
    }
    
    /**
     * Find earliest resource-feasible time with next-event optimization
     */
    private int findEarliestFeasibleTimeOptimized(int j, int earliestStart) {
        int duration = project.p[j];
        
        if (duration == 0) {
            return earliestStart;
        }
        
        // Initialize next resource release tracking
        for (int k = 0; k < project.K; k++) {
            nextResourceRelease[k] = earliestStart;
        }
        
        for (int t = earliestStart; t + duration <= state.T; ) {
            if (isResourceFeasible(j, t)) {
                return t;
            }
            
            // Next-event jump: find next time when limiting resource may be available
            int nextJump = t + 1; // Default increment
            
            for (int k = 0; k < project.K; k++) {
                int demand = project.r[j][k];
                if (demand > 0) {
                    // Find next time this resource has sufficient capacity
                    int nextAvailable = findNextAvailableTime(k, demand, t, t + duration);
                    if (nextAvailable > t) {
                        nextJump = Math.min(nextJump, nextAvailable);
                    }
                }
            }
            
            t = nextJump;
        }
        
        throw new RuntimeException("No feasible schedule found within time horizon for job " + j);
    }
    
    /**
     * Find next time when resource k has at least 'demand' capacity available
     */
    private int findNextAvailableTime(int k, int demand, int startTime, int endTime) {
        for (int t = startTime; t < state.T; t++) {
            boolean hasCapacity = true;
            for (int tau = t; tau < Math.min(endTime, state.T) && hasCapacity; tau++) {
                if (state.avail[k][tau] < demand) {
                    hasCapacity = false;
                }
            }
            if (hasCapacity) {
                return t;
            }
        }
        return state.T; // No capacity found
    }
    
    /**
     * Schedule activity using optimized earliest-feasible placement
     */
    private void scheduleActivity(int j) {
        // Calculate earliest start time from precedence constraints
        int t = Math.max(preprocessor.ESTprec[j], getLatestPredecessorFinish(j));
        
        // Find earliest resource-feasible time (use optimized version)
        t = findEarliestFeasibleTimeOptimized(j, t);
        
        // Schedule the activity
        state.start[j] = t;
        state.finish[j] = t + project.p[j];
        
        // Consume resources (don't reallocate arrays)
        consumeResources(j, t);
    }
    
    /**
     * Get the latest finish time among immediate predecessors
     */
    private int getLatestPredecessorFinish(int j) {
        int latestFinish = 0;
        for (int i = 0; i < project.P[j].size(); i++) {
            int pred = project.P[j].get(i);
            latestFinish = Math.max(latestFinish, state.finish[pred]);
        }
        return latestFinish;
    }
    
    /**
     * Check if activity j can be scheduled at time t (resource feasibility)
     */
    private boolean isResourceFeasible(int j, int t) {
        int duration = project.p[j];
        
        // Check all resources for the entire duration
        for (int k = 0; k < project.K; k++) {
            int demand = project.r[j][k];
            if (demand > 0) {
                for (int tau = t; tau < t + duration && tau < state.T; tau++) {
                    if (state.avail[k][tau] < demand) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Consume resources for activity j scheduled at time t
     */
    private void consumeResources(int j, int t) {
        int duration = project.p[j];
        
        for (int k = 0; k < project.K; k++) {
            int demand = project.r[j][k];
            if (demand > 0) {
                for (int tau = t; tau < t + duration && tau < state.T; tau++) {
                    state.avail[k][tau] -= demand;
                }
            }
        }
    }
    
    /**
     * Get current decoder state (for debugging/analysis)
     */
    public DecoderState getState() {
        return state;
    }
}
