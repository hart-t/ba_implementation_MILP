package heuristics.geneticAlgorithm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Individual {
    int[] activityList;         // precedence-feasible permutation of {1..J} (excludes dummies)
    int makespan;               // fitness after decoding
    int[] start;                // decoded start times[0..J+1] (cached, includes dummies)

    public Individual(int[] activityList) {
        this.activityList = activityList;
        this.makespan = -1;     // not yet computed
        this.start = null;      // not yet computed
    }

    public Individual(int[] activityList, int makespan, int[] start) {
        this.activityList = activityList;
        this.makespan = makespan;
        this.start = start;
    }
    
    /**
     * Check if this individual has been decoded (makespan computed)
     */
    public boolean isDecoded() {
        return makespan >= 0;
    }
    
    /**
     * Get fitness (makespan)
     */
    public int getFitness() {
        return makespan;
    }
    
    /**
     * Get start time for any job (including dummies if cached)
     */
    public int getStartTime(int j) {
        if (start == null) {
            throw new IllegalStateException("Individual not decoded - start times not available");
        }
        if (j < 0 || j >= start.length) {
            throw new IllegalArgumentException("Invalid job index: " + j);
        }
        return start[j];
    }
    
    /**
     * Get finish time for job j (requires project for duration)
     */
    public int getFinishTime(int j, Project project) {
        return start != null ? start[j] + project.p[j] : -1;
    }
    
    /**
     * Check if activity list is valid (contains jobs 1..J exactly once)
     */
    public boolean isValidActivityList(int J) {
        if (activityList.length != J) {
            return false;
        }
        
        boolean[] seen = new boolean[J + 2];
        for (int job : activityList) {
            if (job < 1 || job > J || seen[job]) {
                return false;
            }
            seen[job] = true;
        }
        
        return true;
    }
    
    /**
     * Create a deep copy of this individual
     */
    public Individual clone() {
        return new Individual(
            activityList.clone(),
            makespan,
            start != null ? start.clone() : null
        );
    }
    
    /**
     * Compare fitness with another individual
     */
    public int compareTo(Individual other) {
        return Integer.compare(this.makespan, other.makespan);
    }
    
    @Override
    public String toString() {
        return String.format("Individual{makespan=%d, activityList=%s}", 
                           makespan, Arrays.toString(activityList));
    }

    public Map<Integer,Integer> getStartTimesMap() {
        Map<Integer, Integer> startTimesMap = new HashMap<>();
        for (int i = 0; i < start.length; i++) {
            startTimesMap.put(i, start[i]);
        }
        return startTimesMap;
    }
}
