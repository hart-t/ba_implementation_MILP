package heuristics.geneticAlgorithm;

import java.util.*;

public class ActivityListHelper {
    private final Project project;
    private final ProjectPreprocessor preprocessor;
    
    public ActivityListHelper(Project project) {
        this.project = project;
        this.preprocessor = project.getPreprocessor();
    }
    
    /**
     * Check if swapping adjacent activities maintains precedence feasibility
     */
    public boolean canSwapAdjacent(int[] list, int i) {
        if (i < 0 || i >= list.length - 1) {
            return false;
        }
        
        int a = list[i];
        int b = list[i + 1];
        
        // After swap: b comes before a
        // Illegal if a is a predecessor of b (direct or transitive)
        if (preprocessor.predClosure[b].get(a)) {
            return false;
        }
        
        // Also illegal if b is a predecessor of a (would violate after swap)
        if (preprocessor.predClosure[a].get(b)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Perform adjacent swap if feasible
     */
    public boolean swapAdjacentIfFeasible(int[] list, int i) {
        if (canSwapAdjacent(list, i)) {
            int temp = list[i];
            list[i] = list[i + 1];
            list[i + 1] = temp;
            return true;
        }
        return false;
    }
    
    /**
     * Check if moving job from position 'from' to position 'to' is feasible
     */
    public boolean canMoveJob(int[] list, int from, int to) {
        if (from == to || from < 0 || to < 0 || 
            from >= list.length || to >= list.length) {
            return false;
        }
        
        int job = list[from];
        
        // Check constraints with jobs that will be affected by the move
        if (from < to) {
            // Moving right: job will come after jobs [from+1..to]
            for (int i = from + 1; i <= to; i++) {
                int otherJob = list[i];
                // job cannot be a predecessor of otherJob
                if (preprocessor.predClosure[otherJob].get(job)) {
                    return false;
                }
            }
        } else {
            // Moving left: job will come before jobs [to..from-1]
            for (int i = to; i < from; i++) {
                int otherJob = list[i];
                // otherJob cannot be a predecessor of job
                if (preprocessor.predClosure[job].get(otherJob)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Move job from position 'from' to position 'to' if feasible
     */
    public boolean moveJobIfFeasible(int[] list, int from, int to) {
        if (!canMoveJob(list, from, to)) {
            return false;
        }
        
        int job = list[from];
        
        if (from < to) {
            // Shift left
            System.arraycopy(list, from + 1, list, from, to - from);
        } else {
            // Shift right
            System.arraycopy(list, to, list, to + 1, from - to);
        }
        
        list[to] = job;
        return true;
    }
    
    /**
     * Generate a random precedence-feasible activity list
     */
    public int[] generateRandomFeasibleList(Random random) {
        List<Integer> activities = new ArrayList<>();
        for (int j = 1; j <= project.J; j++) {
            activities.add(j);
        }
        
        List<Integer> result = new ArrayList<>();
        int[] remainingPreds = preprocessor.createPredecessorCountsForSSGS();
        
        // Only count non-dummy predecessors
        for (int j = 1; j <= project.J; j++) {
            remainingPreds[j] = 0;
            for (int i = 0; i < project.P[j].size(); i++) {
                int pred = project.P[j].get(i);
                if (pred != 0) { // Exclude dummy start
                    remainingPreds[j]++;
                }
            }
        }
        
        while (!activities.isEmpty()) {
            // Find eligible activities (no remaining predecessors)
            List<Integer> eligible = new ArrayList<>();
            for (int j : activities) {
                if (remainingPreds[j] == 0) {
                    eligible.add(j);
                }
            }
            
            if (eligible.isEmpty()) {
                throw new RuntimeException("No eligible activities found - precedence graph has cycles");
            }
            
            // Select random eligible activity
            int selected = eligible.get(random.nextInt(eligible.size()));
            result.add(selected);
            activities.remove(Integer.valueOf(selected));
            
            // Update predecessor counts for successors
            for (int i = 0; i < project.S[selected].size(); i++) {
                int succ = project.S[selected].get(i);
                if (succ != project.J + 1) { // Exclude dummy end
                    remainingPreds[succ]--;
                }
            }
        }
        
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Repair an infeasible activity list to make it precedence-feasible
     */
    public int[] repairActivityList(int[] list) {
        int[] result = list.clone();
        int[] position = new int[project.J + 2];
        
        // Build position index
        for (int i = 0; i < result.length; i++) {
            position[result[i]] = i;
        }
        
        boolean changed = true;
        while (changed) {
            changed = false;
            
            for (int j = 1; j <= project.J; j++) {
                // Check all immediate predecessors
                for (int i = 0; i < project.P[j].size(); i++) {
                    int pred = project.P[j].get(i);
                    if (pred != 0 && position[pred] > position[j]) {
                        // Predecessor comes after j - need to fix
                        moveJobIfFeasible(result, position[pred], position[j]);
                        
                        // Rebuild position index
                        for (int k = 0; k < result.length; k++) {
                            position[result[k]] = k;
                        }
                        
                        changed = true;
                        break;
                    }
                }
                if (changed) break;
            }
        }
        
        return result;
    }
    
    /**
     * Check if activity list is completely precedence-feasible
     */
    public boolean isPrecedenceFeasible(int[] list) {
        return preprocessor.isPrecedenceFeasible(list);
    }
    
    /**
     * Find all feasible positions for inserting a job
     */
    public List<Integer> getFeasibleInsertionPositions(int[] list, int job) {
        List<Integer> positions = new ArrayList<>();
        
        for (int pos = 0; pos <= list.length; pos++) {
            // Create temporary list with job inserted at position
            int[] temp = new int[list.length + 1];
            System.arraycopy(list, 0, temp, 0, pos);
            temp[pos] = job;
            System.arraycopy(list, pos, temp, pos + 1, list.length - pos);
            
            if (isPrecedenceFeasible(temp)) {
                positions.add(pos);
            }
        }
        
        return positions;
    }
}
