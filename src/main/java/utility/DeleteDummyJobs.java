package utility;

import io.JobDataInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DeleteDummyJobs {
    
    /**
     * Creates a new JobDataInstance with dummy jobs (supersource and supersink) removed.
     * The supersource is the first job (index 0) and the supersink is the last job.
     * This method properly handles the index shifting and relationship updates.
     * 
     * @param data The original job data instance containing jobs and their relationships.
     * @return A new JobDataInstance with dummy jobs removed.
     */
    public static JobDataInstance deleteDummyJobs(JobDataInstance data) {
        if (data.numberJob < 2) {
            return data; // Return original data if not enough jobs to have dummy jobs
        }
        
        // Identify dummy jobs (jobs with duration 0)
        // Typically the first job (supersource) and last job (supersink) are dummy jobs
        int supersourceIndex = 0;
        int supersinkIndex = data.numberJob - 1;
        
        // Verify these are indeed dummy jobs
        if (data.jobDuration.get(supersourceIndex) != 0 || data.jobDuration.get(supersinkIndex) != 0) {
            throw new IllegalArgumentException("Expected dummy jobs at first and last positions with duration 0");
        }
        
        // Create new data structures without dummy jobs
        int newNumberJob = data.numberJob - 2;
        List<Integer> newJobNumSuccessors = new ArrayList<>();
        List<List<Integer>> newJobSuccessors = new ArrayList<>();
        List<List<Integer>> newJobPredecessors = new ArrayList<>();
        List<Integer> newJobDuration = new ArrayList<>();
        List<List<Integer>> newJobResource = new ArrayList<>();
        
        // Copy non-dummy jobs (skip first and last)
        for (int i = 1; i < data.numberJob - 1; i++) {
            newJobDuration.add(data.jobDuration.get(i));
            newJobResource.add(new ArrayList<>(data.jobResource.get(i)));
            
            // Process successors - exclude dummy jobs and adjust indices
            List<Integer> originalSuccessors = data.jobSuccessors.get(i);
            List<Integer> newSuccessors = new ArrayList<>();
            int numSuccessors = 0;
            
            for (int successor : originalSuccessors) {
                int successorIndex = successor - 1; // Convert to 0-based
                // Skip if successor is a dummy job
                if (successorIndex != supersourceIndex && successorIndex != supersinkIndex) {
                    // Adjust for removed supersource and convert to 0-based indexing
                    int adjustedSuccessor = successorIndex - 1; // Reduce by 1 for supersource removal
                    newSuccessors.add(adjustedSuccessor);
                    numSuccessors++;
                }
            }
            
            newJobSuccessors.add(newSuccessors);
            newJobNumSuccessors.add(numSuccessors);
        }
        
        // Initialize predecessors for all non-dummy jobs
        for (int i = 0; i < newNumberJob; i++) {
            newJobPredecessors.add(new ArrayList<>());
        }
        
        // Build predecessor relationships from successor relationships
        for (int job = 0; job < newNumberJob; job++) {
            for (int successor : newJobSuccessors.get(job)) {
                // successor is already 0-based and adjusted for removed dummy jobs
                if (successor >= 0 && successor < newNumberJob) {
                    newJobPredecessors.get(successor).add(job); // Use 0-based indexing
                }
            }
        }


        

        // Create and return new JobDataInstance with dummy jobs removed
        return new JobDataInstance(
            data.instanceName, // Keep the same instance name
            newNumberJob,
            data.horizon, // Keep the same horizon
            newJobNumSuccessors,
            newJobSuccessors,
            newJobPredecessors,
            newJobDuration,
            newJobResource,
            new ArrayList<>(data.resourceCapacity) // Copy resource capacity
        );
    }

    /**
     * Deletes dummy jobs from a map of start times.
     * 
     * @param startTimes The original map of job start times.
     * @return A new map with dummy jobs removed.
     */
    public static Map<Integer, Integer> deleteDummyJobsFromStartTimesMap(Map<Integer, Integer> startTimes) {
        Map<Integer, Integer> modifiedStartTimes = new HashMap<>();
        
        // Find the maximum job index to identify supersink
        int maxJobIndex = startTimes.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int supersourceIndex = 0; // First job is supersource
        int supersinkIndex = maxJobIndex; // Last job is supersink
        
        // Copy all start times except for dummy jobs, and adjust indices
        for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
            int jobIndex = entry.getKey();
            int startTime = entry.getValue();
            
            // Skip dummy jobs
            if (jobIndex == supersourceIndex || jobIndex == supersinkIndex) {
                continue;
            }
            
            // Adjust job index (subtract 1 because we're removing supersource)
            int newJobIndex = jobIndex > supersourceIndex ? jobIndex - 1 : jobIndex;
            modifiedStartTimes.put(newJobIndex, startTime);
        }
        
        return modifiedStartTimes;
    }

    public static int[][] deleteDummyJobsFromEarliestLatestStartTimes(int[][] earliestLatestStartTimes) {
        // Remove the first and last elements from both earliest and latest start time arrays
        int newLength = earliestLatestStartTimes[0].length - 2;
        int[][] modifiedTimes = new int[2][newLength];
        
        // Copy earliest start times (skip first and last elements)
        for (int i = 0; i < newLength; i++) {
            modifiedTimes[0][i] = earliestLatestStartTimes[0][i + 1];
        }
        
        // Copy latest start times (skip first and last elements)
        for (int i = 0; i < newLength; i++) {
            modifiedTimes[1][i] = earliestLatestStartTimes[1][i + 1];
        }
        
        return modifiedTimes;
    }
}
