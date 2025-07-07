package utility;

import io.JobDataInstance;
import io.FileReader;

public class DeleteDummyJobsRealDataTest {
    
    public static void main(String[] args) {
        System.out.println("Testing DeleteDummyJobs with real file data...");
        
        try {
            // Read the real file
            FileReader reader = new FileReader();
            JobDataInstance originalData = reader.dataRead("/home/tobsi/university/kit/benchmarkSets/j303_3.sm");
            
            System.out.println("Original data:");
            System.out.println("  Number of jobs: " + originalData.numberJob);
            System.out.println("  First job duration: " + originalData.jobDuration.get(0));
            System.out.println("  Last job duration: " + originalData.jobDuration.get(originalData.numberJob - 1));
            System.out.println("  Horizon: " + originalData.horizon);
            
            // Apply dummy job deletion
            JobDataInstance cleanedData = DeleteDummyJobs.deleteDummyJobs(originalData);
            
            System.out.println("\nCleaned data:");
            System.out.println("  Number of jobs: " + cleanedData.numberJob);
            System.out.println("  First job duration: " + cleanedData.jobDuration.get(0));
            System.out.println("  Last job duration: " + cleanedData.jobDuration.get(cleanedData.numberJob - 1));
            System.out.println("  Horizon: " + cleanedData.horizon);
            
            // Verify the results
            boolean passed = true;
            
            // Check that dummy jobs were removed
            if (originalData.numberJob != cleanedData.numberJob + 2) {
                System.out.println("❌ FAIL: Expected " + (originalData.numberJob - 2) + " jobs, got " + cleanedData.numberJob);
                passed = false;
            } else {
                System.out.println("✅ PASS: Correct number of jobs after dummy removal");
            }
            
            // Check that no job has duration 0
            for (int duration : cleanedData.jobDuration) {
                if (duration == 0) {
                    System.out.println("❌ FAIL: Found job with duration 0 in cleaned data");
                    passed = false;
                    break;
                }
            }
            if (passed) {
                System.out.println("✅ PASS: No jobs with duration 0 in cleaned data");
            }
            
            // Check that original data is unchanged
            if (originalData.numberJob != 32) {
                System.out.println("❌ FAIL: Original data was modified");
                passed = false;
            } else {
                System.out.println("✅ PASS: Original data remains unchanged");
            }
            
            // Check that horizon and resource capacities are preserved
            if (!originalData.resourceCapacity.equals(cleanedData.resourceCapacity)) {
                System.out.println("❌ FAIL: Resource capacities were not preserved");
                passed = false;
            } else {
                System.out.println("✅ PASS: Resource capacities preserved");
            }
            
            if (originalData.horizon != cleanedData.horizon) {
                System.out.println("❌ FAIL: Horizon was not preserved");
                passed = false;
            } else {
                System.out.println("✅ PASS: Horizon preserved");
            }
            
            // Check that all successor/predecessor relationships are valid
            boolean relationshipsValid = true;
            for (int job = 0; job < cleanedData.numberJob; job++) {
                for (int successor : cleanedData.jobSuccessors.get(job)) {
                    if (successor < 1 || successor > cleanedData.numberJob) {
                        System.out.println("❌ FAIL: Invalid successor " + successor + " for job " + (job + 1));
                        relationshipsValid = false;
                    }
                }
                for (int predecessor : cleanedData.jobPredecessors.get(job)) {
                    if (predecessor < 1 || predecessor > cleanedData.numberJob) {
                        System.out.println("❌ FAIL: Invalid predecessor " + predecessor + " for job " + (job + 1));
                        relationshipsValid = false;
                    }
                }
            }
            if (relationshipsValid) {
                System.out.println("✅ PASS: All successor/predecessor relationships are valid");
            } else {
                passed = false;
            }
            
            if (passed) {
                System.out.println("\n🎉 ALL REAL DATA TESTS PASSED! The DeleteDummyJobs works correctly with real file data.");
            } else {
                System.out.println("\n❌ Some real data tests failed.");
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
