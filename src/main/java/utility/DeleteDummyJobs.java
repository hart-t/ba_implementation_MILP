package utility;

import io.JobDataInstance;
import java.util.ArrayList;
import java.util.List;

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

        System.out.println("New job predecessors: " + data.jobPredecessors);

        
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
                    newSuccessors.add(successorIndex);
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
                int successorIndex = successor - 1; // Convert to 0-based
                if (successorIndex >= 0 && successorIndex < newNumberJob) {
                    newJobPredecessors.get(successorIndex).add(job + 1); // Convert back to 1-based
                }
            }
        }

        System.out.println("New job predecessors: " + newJobPredecessors);
        
        // Create and return new JobDataInstance with dummy jobs removed
        return new JobDataInstance(
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
}
