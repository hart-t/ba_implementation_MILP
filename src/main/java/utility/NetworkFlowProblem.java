package utility;

import java.util.*;
import io.JobDataInstance;

/*
 * This file is part of the BA Implementation MILP project.
 * Build based on the model by Jonas Saupe.
 */

public class NetworkFlowProblem {

    /**
     * Finds flow variables for the network flow problem based on the mathematical formulation:
     * - Constraint (1): Sum of flows from source equals resource capacity
     * - Constraint (2): Flow conservation for each node (inflow = outflow = demand)
     * - Constraint (3): Sum of flows to sink equals resource capacity
     */
    public int[][][] findFlowVariables(JobDataInstance data, Map<Integer, Integer> startTimes) {
        int numJobs = data.numberJob;
        int numResources = data.resourceCapacity.size();
        
        // Initialize flow variables array
        int[][][] flowVariables = new int[numJobs][numJobs][numResources];
        
        // Find maximum time (last job start time + duration)
        int maxTime = 0;
        for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
            int jobId = entry.getKey();
            int startTime = entry.getValue();
            int endTime = startTime + data.jobDuration.get(jobId);
            maxTime = Math.max(maxTime, endTime);
        }
        
        for (int resourceId = 0; resourceId < numResources; resourceId++) {
            // System.out.println("\n=== Processing Resource " + resourceId + " ===");
            
            // Step 1: Collect jobs that need this resource
            List<Integer> jobsNeedingResource = new ArrayList<>();
            Map<Integer, Integer> jobResourceDemand = new HashMap<>();
            
            for (int jobId = 0; jobId < numJobs; jobId++) {
                int demand = data.jobResource.get(jobId).get(resourceId);
                if (demand > 0) {
                    jobsNeedingResource.add(jobId);
                    jobResourceDemand.put(jobId, demand);
                }
            }
            
            //System.out.println("Jobs needing resource " + resourceId + ": " + jobsNeedingResource);
            
            // Step 2: Create and fill matrix
            int[][] resourceMatrix = new int[numJobs][maxTime];
            
            for (int jobId : jobsNeedingResource) {
                int startTime = startTimes.get(jobId);
                int duration = data.jobDuration.get(jobId);
                int demand = jobResourceDemand.get(jobId);
                
                for (int time = startTime; time < startTime + duration; time++) {
                    resourceMatrix[jobId][time] = demand;
                }
            }
            
            /*
             * // Print matrix
            System.out.println("Resource usage matrix for resource " + resourceId + ":");
            System.out.print("Job\\Time: ");
            for (int t = 0; t < maxTime; t++) {
                System.out.printf("%3d", t);
            }
            System.out.println();
            
            for (int jobId = 0; jobId < numJobs; jobId++) {
                System.out.printf("Job %2d:   ", jobId);
                for (int t = 0; t < maxTime; t++) {
                    System.out.printf("%3d", resourceMatrix[jobId][t]);
                }
                System.out.println();
            }
             */
            
            // Step 3: Initialize availability map with job 0 (source)
            Map<Integer, Integer> availableResources = new HashMap<>();
            availableResources.put(0, data.resourceCapacity.get(resourceId));
            
            // System.out.println("Initial availability: " + availableResources);
            
            // Step 4: Process each time point
            Set<Integer> processedJobs = new HashSet<>();
            processedJobs.add(0); // Job 0 is the source, already processed
            
            for (int currentTime = 0; currentTime < maxTime; currentTime++) {
                // System.out.println("\nTime " + currentTime + ":");
                
                // Check which jobs start at this time and need resources
                for (int jobId : jobsNeedingResource) {
                    if (processedJobs.contains(jobId)) continue;
                    
                    int jobStartTime = startTimes.get(jobId);
                    if (jobStartTime == currentTime) {
                        int requiredAmount = jobResourceDemand.get(jobId);
                        // System.out.println("  Job " + jobId + " starts and needs " + requiredAmount + " units");
                        
                        // Create flow variables from available jobs to this job
                        int remainingDemand = requiredAmount;
                        List<Integer> sortedAvailableJobs = new ArrayList<>(availableResources.keySet());
                        sortedAvailableJobs.sort(Collections.reverseOrder()); // Start with highest index
                        
                        for (int sourceJob : sortedAvailableJobs) {
                            if (remainingDemand <= 0) break;
                            
                            int availableAmount = availableResources.get(sourceJob);
                            int flowAmount = Math.min(availableAmount, remainingDemand);
                            
                            if (flowAmount > 0) {
                                flowVariables[sourceJob][jobId][resourceId] = flowAmount;
                                // System.out.println("    Flow: " + flowAmount + " units from job " + sourceJob + " to job " + jobId);
                                
                                // Update availability
                                availableResources.put(sourceJob, availableAmount - flowAmount);
                                if (availableResources.get(sourceJob) == 0) {
                                    availableResources.remove(sourceJob);
                                }
                                remainingDemand -= flowAmount;
                            }
                        }
                        
                        if (remainingDemand > 0) {
                            System.out.println("    WARNING: Could not satisfy full demand for job " + jobId);
                        }
                        
                        processedJobs.add(jobId);
                        
                        // Clear this job's demand from matrix
                        for (int t = 0; t < maxTime; t++) {
                            resourceMatrix[jobId][t] = 0;
                        }
                    }
                }
                
                // Check which jobs end at this time and add them to availability
                for (int jobId : jobsNeedingResource) {
                    if (jobId == 0) continue; // Skip source job
                    
                    int jobStartTime = startTimes.get(jobId);
                    int jobDuration = data.jobDuration.get(jobId);
                    int jobEndTime = jobStartTime + jobDuration;
                    
                    if (jobEndTime == currentTime + 1 && processedJobs.contains(jobId)) {
                        int usedAmount = jobResourceDemand.get(jobId);
                        availableResources.put(jobId, usedAmount);
                        // System.out.println("  Job " + jobId + " ends, adding " + usedAmount + " units to availability");
                    }
                }
                
                // System.out.println("  Current availability: " + availableResources);
            }
            
            // Step 5: Handle the last job (sink)
            int lastJobId = numJobs - 1;
            if (availableResources.size() > 0) {
                // System.out.println("\nTransferring remaining resources to sink (job " + lastJobId + "):");
                for (Map.Entry<Integer, Integer> entry : availableResources.entrySet()) {
                    int sourceJob = entry.getKey();
                    int availableAmount = entry.getValue();
                    if (availableAmount > 0) {
                        flowVariables[sourceJob][lastJobId][resourceId] = availableAmount;
                        // System.out.println("  Flow: " + availableAmount + " units from job " + sourceJob + " to sink");
                    }
                }
            }
        }
        
        /*
         * // Print all flow variables
        System.out.println("\n=== ALL FLOW VARIABLES ===");
        for (int resourceId = 0; resourceId < numResources; resourceId++) {
            System.out.println("\nResource " + resourceId + " flows:");
            boolean hasFlow = false;
            for (int i = 0; i < numJobs; i++) {
                for (int j = 0; j < numJobs; j++) {
                    if (flowVariables[i][j][resourceId] > 0) {
                        System.out.println("  Flow[" + i + "][" + j + "][" + resourceId + "] = " + flowVariables[i][j][resourceId]);
                        hasFlow = true;
                    }
                }
            }
            if (!hasFlow) {
                System.out.println("  No flows for this resource");
            }
        }
         */
        
        // Validation checks
        // System.out.println("\n=== FLOW VALIDATION ===");
        for (int resourceId = 0; resourceId < numResources; resourceId++) {
            // System.out.println("\nValidating resource " + resourceId + ":");
            
            // Check 1: Source output equals resource capacity
            int sourceOutput = 0;
            for (int j = 0; j < numJobs; j++) {
                sourceOutput += flowVariables[0][j][resourceId];
            }
            int capacity = data.resourceCapacity.get(resourceId);
            
            if (sourceOutput != capacity) {
                System.out.println("  WARNING: Constraint 1 violated! Source output (" + sourceOutput + 
                                 ") != resource capacity (" + capacity + ")");
            } else {
                // System.out.println("  ✓ Constraint 1 satisfied: Source output = " + sourceOutput);
            }
            
            // Check 2: Flow conservation for each node
            for (int nodeId = 1; nodeId < numJobs - 1; nodeId++) {
                int inputFlow = 0;
                int outputFlow = 0;
                
                // Calculate input flow
                for (int i = 0; i < numJobs; i++) {
                    inputFlow += flowVariables[i][nodeId][resourceId];
                }
                
                // Calculate output flow
                for (int j = 0; j < numJobs; j++) {
                    outputFlow += flowVariables[nodeId][j][resourceId];
                }
                
                // Get resource demand
                int demand = data.jobResource.get(nodeId).get(resourceId);
                
                if (inputFlow != outputFlow || inputFlow != demand) {
                    System.out.println("  WARNING: Constraint 2 violated for node " + nodeId + 
                                     "! Input (" + inputFlow + ") != Output (" + outputFlow + 
                                     ") != Demand (" + demand + ")");
                } else if (demand > 0) {
                    // System.out.println("  ✓ Constraint 2 satisfied for node " + nodeId + 
                                     // ": Input = Output = Demand = " + demand);
                }
            }
            
            // Check 3: Sink input equals resource capacity
            int sinkInput = 0;
            int lastJobId = numJobs - 1;
            for (int i = 0; i < numJobs; i++) {
                sinkInput += flowVariables[i][lastJobId][resourceId];
            }
            
            if (sinkInput != capacity) {
                System.out.println("  WARNING: Constraint 3 violated! Sink input (" + sinkInput + 
                                 ") != resource capacity (" + capacity + ")");
            } else {
                // System.out.println("  ✓ Constraint 3 satisfied: Sink input = " + sinkInput);
            }
        }
        
        return flowVariables;
    }
}