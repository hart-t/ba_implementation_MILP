package io;

import java.util.*;



public class JobDataInstance {
        public final int numberJob;
        public final int horizon;
        public final List<Integer> jobNumSuccessors;
        public final List<List<Integer>> jobSuccessors;
        public final List<List<Integer>> jobPredecessors;
        public final List<Integer> jobDuration;
        public final List<List<Integer>> jobResource;
        public final List<Integer> resourceCapacity;
        private Map<Integer, Integer> startTimes;
        
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

        public void setStartTimes(Map<Integer, Integer> startTimes) {
            this.startTimes = startTimes;
        }

        public Map<Integer, Integer> getStartTimes() {
            return startTimes;
        }
    }