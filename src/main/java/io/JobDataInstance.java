package io;

import java.util.*;



public class JobDataInstance {
    final public int numberJob;
    final public int horizon;
    final public List<Integer> jobNumSuccessors;
    final public List<List<Integer>> jobSuccessors;
    final public List<List<Integer>> jobPredecessors;
    final public List<Integer> jobDuration;
    final public List<List<Integer>> jobResource;
    final public List<Integer> resourceCapacity;
    
    public JobDataInstance(int numberJob, int horizon, List<Integer> jobNumSuccessors, 
                                List<List<Integer>> jobSuccessors, List<List<Integer>> jobPredecessors,
                                List<Integer> jobDuration, List<List<Integer>> jobResource,
                                List<Integer> resourceCapacity) {

        this.numberJob = numberJob;
        this.horizon = horizon;
        this.jobNumSuccessors = jobNumSuccessors;
        this.jobSuccessors = jobSuccessors;
        this.jobPredecessors = jobPredecessors;
        this.jobDuration = jobDuration;
        this.jobResource = jobResource;
        this.resourceCapacity = resourceCapacity;
    }
}