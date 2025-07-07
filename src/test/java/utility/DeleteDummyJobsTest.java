package utility;

import io.JobDataInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeleteDummyJobsTest {
    
    private JobDataInstance testData;
    
    @BeforeEach
    void setUp() {
        // Create test data similar to the j303_3.sm file structure
        // Job 1 (index 0) = supersource (dummy), duration 0
        // Job 2 (index 1) = real job, duration 3
        // Job 3 (index 2) = real job, duration 1
        // Job 4 (index 3) = real job, duration 6
        // Job 5 (index 4) = supersink (dummy), duration 0
        
        int numberJob = 5;
        int horizon = 20;
        
        // Job durations: [0, 3, 1, 6, 0] - first and last are dummy jobs
        List<Integer> jobDuration = Arrays.asList(0, 3, 1, 6, 0);
        
        // Job successors (1-based indexing):
        // Job 1 -> [2, 3] (supersource connects to jobs 2 and 3)
        // Job 2 -> [4] (job 2 connects to job 4)
        // Job 3 -> [4] (job 3 connects to job 4)
        // Job 4 -> [5] (job 4 connects to supersink)
        // Job 5 -> [] (supersink has no successors)
        List<List<Integer>> jobSuccessors = Arrays.asList(
            Arrays.asList(2, 3),    // Job 1 successors
            Arrays.asList(4),       // Job 2 successors
            Arrays.asList(4),       // Job 3 successors
            Arrays.asList(5),       // Job 4 successors
            Arrays.asList()         // Job 5 successors (empty)
        );
        
        // Number of successors for each job
        List<Integer> jobNumSuccessors = Arrays.asList(2, 1, 1, 1, 0);
        
        // Job predecessors (1-based indexing):
        // Job 1 -> [] (supersource has no predecessors)
        // Job 2 -> [1] (job 2 has supersource as predecessor)
        // Job 3 -> [1] (job 3 has supersource as predecessor)
        // Job 4 -> [2, 3] (job 4 has jobs 2 and 3 as predecessors)
        // Job 5 -> [4] (supersink has job 4 as predecessor)
        List<List<Integer>> jobPredecessors = Arrays.asList(
            Arrays.asList(),        // Job 1 predecessors (empty)
            Arrays.asList(1),       // Job 2 predecessors
            Arrays.asList(1),       // Job 3 predecessors
            Arrays.asList(2, 3),    // Job 4 predecessors
            Arrays.asList(4)        // Job 5 predecessors
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
        
        testData = new JobDataInstance(
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
    
    @Test
    void testDeleteDummyJobs() {
        // Act
        JobDataInstance result = DeleteDummyJobs.deleteDummyJobs(testData);
        
        // Assert basic properties
        assertEquals(3, result.numberJob, "Number of jobs should be reduced by 2 (from 5 to 3)");
        assertEquals(testData.horizon, result.horizon, "Horizon should remain unchanged");
        assertEquals(testData.resourceCapacity, result.resourceCapacity, "Resource capacity should remain unchanged");
        
        // Verify job durations (dummy jobs with duration 0 should be removed)
        List<Integer> expectedDurations = Arrays.asList(3, 1, 6);
        assertEquals(expectedDurations, result.jobDuration, "Job durations should exclude dummy jobs");
        
        // Verify job resources (dummy jobs should be removed)
        List<List<Integer>> expectedResources = Arrays.asList(
            Arrays.asList(0, 0, 5, 0),  // Original job 2
            Arrays.asList(1, 0, 0, 0),  // Original job 3
            Arrays.asList(0, 0, 8, 0)   // Original job 4
        );
        assertEquals(expectedResources, result.jobResource, "Job resources should exclude dummy jobs");
        
        // Verify successors (indices should be adjusted)
        // Original job 2 (now job 1) -> [3] becomes [3] (original job 4 becomes job 3)
        // Original job 3 (now job 2) -> [3] becomes [3] (original job 4 becomes job 3)
        // Original job 4 (now job 3) -> [] (supersink reference removed)
        List<List<Integer>> expectedSuccessors = Arrays.asList(
            Arrays.asList(3),    // Job 1 (was job 2) -> job 3 (was job 4)
            Arrays.asList(3),    // Job 2 (was job 3) -> job 3 (was job 4)
            Arrays.asList()      // Job 3 (was job 4) -> no successors (supersink removed)
        );
        assertEquals(expectedSuccessors, result.jobSuccessors, "Job successors should be adjusted for removed dummy jobs");
        
        // Verify number of successors
        List<Integer> expectedNumSuccessors = Arrays.asList(1, 1, 0);
        assertEquals(expectedNumSuccessors, result.jobNumSuccessors, "Number of successors should be updated");
        
        // Verify predecessors (indices should be adjusted)
        // Job 1 (was job 2) -> [] (supersource reference removed)
        // Job 2 (was job 3) -> [] (supersource reference removed)
        // Job 3 (was job 4) -> [1, 2] (original jobs 2 and 3 become jobs 1 and 2)
        List<List<Integer>> expectedPredecessors = Arrays.asList(
            Arrays.asList(),        // Job 1 (was job 2) -> no predecessors (supersource removed)
            Arrays.asList(),        // Job 2 (was job 3) -> no predecessors (supersource removed)
            Arrays.asList(1, 2)     // Job 3 (was job 4) -> jobs 1 and 2 (were jobs 2 and 3)
        );
        assertEquals(expectedPredecessors, result.jobPredecessors, "Job predecessors should be adjusted for removed dummy jobs");
    }
    
    @Test
    void testDeleteDummyJobsWithInsufficientJobs() {
        // Arrange - create data with only 1 job
        JobDataInstance smallData = new JobDataInstance(
            1, 10, 
            Arrays.asList(0),
            Arrays.asList(Arrays.asList()),
            Arrays.asList(Arrays.asList()),
            Arrays.asList(5),
            Arrays.asList(Arrays.asList(1, 2, 3, 4)),
            Arrays.asList(10, 10, 10, 10)
        );
        
        // Act
        JobDataInstance result = DeleteDummyJobs.deleteDummyJobs(smallData);
        
        // Assert - should return the same instance
        assertSame(smallData, result, "Should return original data when insufficient jobs");
    }
    
    @Test
    void testDeleteDummyJobsWithNonDummyJobs() {
        // Arrange - create data where first and last jobs are not dummy jobs (duration != 0)
        List<Integer> nonDummyDurations = Arrays.asList(5, 3, 1, 6, 4); // No zero durations
        JobDataInstance nonDummyData = new JobDataInstance(
            5, testData.horizon,
            testData.jobNumSuccessors,
            testData.jobSuccessors,
            testData.jobPredecessors,
            nonDummyDurations,
            testData.jobResource,
            testData.resourceCapacity
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            DeleteDummyJobs.deleteDummyJobs(nonDummyData);
        }, "Should throw exception when first and last jobs are not dummy jobs");
    }
    
    @Test
    void testOriginalDataUnchanged() {
        // Store original values
        int originalNumberJob = testData.numberJob;
        List<Integer> originalDurations = new ArrayList<>(testData.jobDuration);
        List<List<Integer>> originalSuccessors = new ArrayList<>();
        for (List<Integer> successors : testData.jobSuccessors) {
            originalSuccessors.add(new ArrayList<>(successors));
        }
        
        // Act
        JobDataInstance result = DeleteDummyJobs.deleteDummyJobs(testData);
        
        // Assert original data is unchanged
        assertEquals(originalNumberJob, testData.numberJob, "Original number of jobs should be unchanged");
        assertEquals(originalDurations, testData.jobDuration, "Original job durations should be unchanged");
        assertEquals(originalSuccessors, testData.jobSuccessors, "Original job successors should be unchanged");
        
        // Verify result is different from original
        assertNotSame(testData, result, "Result should be a new instance");
        assertNotEquals(testData.numberJob, result.numberJob, "Result should have different number of jobs");
    }
}
