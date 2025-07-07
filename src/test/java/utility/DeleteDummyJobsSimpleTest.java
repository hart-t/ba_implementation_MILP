package utility;

import io.JobDataInstance;
import java.util.Arrays;
import java.util.List;

public class DeleteDummyJobsSimpleTest {
    
    public static void main(String[] args) {
        System.out.println("Testing DeleteDummyJobs functionality...");
        
        // Create test data similar to the j303_3.sm file structure
        JobDataInstance testData = createTestData();
        
        // Test the deletion
        try {
            JobDataInstance result = DeleteDummyJobs.deleteDummyJobs(testData);
            
            // Run assertions
            boolean allTestsPassed = true;
            
            // Test 1: Check number of jobs
            if (result.numberJob != 3) {
                System.out.println("❌ FAIL: Expected 3 jobs, got " + result.numberJob);
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Number of jobs correctly reduced to 3");
            }
            
            // Test 2: Check job durations (should exclude dummy jobs)
            List<Integer> expectedDurations = Arrays.asList(3, 1, 6);
            if (!expectedDurations.equals(result.jobDuration)) {
                System.out.println("❌ FAIL: Expected durations " + expectedDurations + ", got " + result.jobDuration);
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Job durations correctly exclude dummy jobs");
            }
            
            // Test 3: Check successors (indices should be adjusted)
            List<List<Integer>> expectedSuccessors = Arrays.asList(
                Arrays.asList(3),    // Job 1 (was job 2) -> job 3 (was job 4)
                Arrays.asList(3),    // Job 2 (was job 3) -> job 3 (was job 4)
                Arrays.asList()      // Job 3 (was job 4) -> no successors (supersink removed)
            );
            if (!expectedSuccessors.equals(result.jobSuccessors)) {
                System.out.println("❌ FAIL: Expected successors " + expectedSuccessors + ", got " + result.jobSuccessors);
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Job successors correctly adjusted");
            }
            
            // Test 4: Check predecessors (indices should be adjusted)
            List<List<Integer>> expectedPredecessors = Arrays.asList(
                Arrays.asList(),        // Job 1 (was job 2) -> no predecessors (supersource removed)
                Arrays.asList(),        // Job 2 (was job 3) -> no predecessors (supersource removed)
                Arrays.asList(1, 2)     // Job 3 (was job 4) -> jobs 1 and 2 (were jobs 2 and 3)
            );
            if (!expectedPredecessors.equals(result.jobPredecessors)) {
                System.out.println("❌ FAIL: Expected predecessors " + expectedPredecessors + ", got " + result.jobPredecessors);
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Job predecessors correctly adjusted");
            }
            
            // Test 5: Check number of successors
            List<Integer> expectedNumSuccessors = Arrays.asList(1, 1, 0);
            if (!expectedNumSuccessors.equals(result.jobNumSuccessors)) {
                System.out.println("❌ FAIL: Expected num successors " + expectedNumSuccessors + ", got " + result.jobNumSuccessors);
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Number of successors correctly updated");
            }
            
            // Test 6: Check that original data is unchanged
            if (testData.numberJob != 5) {
                System.out.println("❌ FAIL: Original data was modified");
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Original data remains unchanged");
            }
            
            // Test 7: Check job resources
            List<List<Integer>> expectedResources = Arrays.asList(
                Arrays.asList(0, 0, 5, 0),  // Original job 2
                Arrays.asList(1, 0, 0, 0),  // Original job 3
                Arrays.asList(0, 0, 8, 0)   // Original job 4
            );
            if (!expectedResources.equals(result.jobResource)) {
                System.out.println("❌ FAIL: Expected resources " + expectedResources + ", got " + result.jobResource);
                allTestsPassed = false;
            } else {
                System.out.println("✅ PASS: Job resources correctly copied");
            }
            
            // Test edge cases
            testEdgeCases();
            
            if (allTestsPassed) {
                System.out.println("\n🎉 ALL TESTS PASSED! The DeleteDummyJobs functionality works correctly.");
            } else {
                System.out.println("\n❌ Some tests failed. Please check the implementation.");
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static JobDataInstance createTestData() {
        // Create test data with dummy jobs at first and last positions
        int numberJob = 5;
        int horizon = 20;
        
        // Job durations: [0, 3, 1, 6, 0] - first and last are dummy jobs
        List<Integer> jobDuration = Arrays.asList(0, 3, 1, 6, 0);
        
        // Job successors (1-based indexing):
        List<List<Integer>> jobSuccessors = Arrays.asList(
            Arrays.asList(2, 3),    // Job 1 -> [2, 3] (supersource connects to jobs 2 and 3)
            Arrays.asList(4),       // Job 2 -> [4] (job 2 connects to job 4)
            Arrays.asList(4),       // Job 3 -> [4] (job 3 connects to job 4)
            Arrays.asList(5),       // Job 4 -> [5] (job 4 connects to supersink)
            Arrays.asList()         // Job 5 -> [] (supersink has no successors)
        );
        
        // Number of successors for each job
        List<Integer> jobNumSuccessors = Arrays.asList(2, 1, 1, 1, 0);
        
        // Job predecessors (1-based indexing):
        List<List<Integer>> jobPredecessors = Arrays.asList(
            Arrays.asList(),        // Job 1 -> [] (supersource has no predecessors)
            Arrays.asList(1),       // Job 2 -> [1] (job 2 has supersource as predecessor)
            Arrays.asList(1),       // Job 3 -> [1] (job 3 has supersource as predecessor)
            Arrays.asList(2, 3),    // Job 4 -> [2, 3] (job 4 has jobs 2 and 3 as predecessors)
            Arrays.asList(4)        // Job 5 -> [4] (supersink has job 4 as predecessor)
        );
        
        // Job resource requirements (4 resources per job)
        List<List<Integer>> jobResource = Arrays.asList(
            Arrays.asList(0, 0, 0, 0),  // Job 1 resources (dummy)
            Arrays.asList(0, 0, 5, 0),  // Job 2 resources
            Arrays.asList(1, 0, 0, 0),  // Job 3 resources
            Arrays.asList(0, 0, 8, 0),  // Job 4 resources
            Arrays.asList(0, 0, 0, 0)   // Job 5 resources (dummy)
        );
        
        // Resource capacity
        List<Integer> resourceCapacity = Arrays.asList(15, 10, 12, 18);
        
        return new JobDataInstance(
            numberJob,
            horizon,
            jobNumSuccessors,
            jobSuccessors,
            jobPredecessors,
            jobDuration,
            jobResource,
            resourceCapacity
        );
    }
    
    private static void testEdgeCases() {
        System.out.println("\nTesting edge cases...");
        
        // Test 1: Data with insufficient jobs
        JobDataInstance smallData = new JobDataInstance(
            1, 10, 
            Arrays.asList(0),
            Arrays.asList(Arrays.asList()),
            Arrays.asList(Arrays.asList()),
            Arrays.asList(5),
            Arrays.asList(Arrays.asList(1, 2, 3, 4)),
            Arrays.asList(10, 10, 10, 10)
        );
        
        JobDataInstance smallResult = DeleteDummyJobs.deleteDummyJobs(smallData);
        if (smallResult == smallData) {
            System.out.println("✅ PASS: Returns original data when insufficient jobs");
        } else {
            System.out.println("❌ FAIL: Should return original data when insufficient jobs");
        }
        
        // Test 2: Data with non-dummy jobs at first/last positions
        List<Integer> nonDummyDurations = Arrays.asList(5, 3, 1, 6, 4); // No zero durations
        JobDataInstance nonDummyData = new JobDataInstance(
            5, 20,
            Arrays.asList(2, 1, 1, 1, 0),
            Arrays.asList(Arrays.asList(2, 3), Arrays.asList(4), Arrays.asList(4), Arrays.asList(5), Arrays.asList()),
            Arrays.asList(Arrays.asList(), Arrays.asList(1), Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)),
            nonDummyDurations,
            Arrays.asList(Arrays.asList(1,2,3,4), Arrays.asList(1,2,3,4), Arrays.asList(1,2,3,4), Arrays.asList(1,2,3,4), Arrays.asList(1,2,3,4)),
            Arrays.asList(10, 10, 10, 10)
        );
        
        try {
            DeleteDummyJobs.deleteDummyJobs(nonDummyData);
            System.out.println("❌ FAIL: Should throw exception for non-dummy jobs at first/last positions");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ PASS: Correctly throws exception for non-dummy jobs at first/last positions");
        }
    }
}
