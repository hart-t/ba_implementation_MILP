package models;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileReader {
    
    public static class JobData {
        public final int numberJob;
        public final int horizon;
        public final List<Integer> jobNumSuccessors;
        public final List<List<Integer>> jobSuccessors;
        public final List<List<Integer>> jobPredecessors;
        public final List<Integer> jobDuration;
        public final List<List<Integer>> jobResource;
        public final List<Integer> resourceCapacity;
        
        public JobData(int numberJob, int horizon, List<Integer> jobNumSuccessors,
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

    public JobData dataRead(String file) throws Exception {
        System.out.println("Reading file: " + file);
        List<String[]> data = new ArrayList<>();
        
        // Read file and split lines
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                data.add(parts);
            }
        }

        List<Integer> jobNumSuccessors = new ArrayList<>();
        List<List<Integer>> jobSuccessors = new ArrayList<>();
        List<List<Integer>> jobPredecessors = new ArrayList<>();
        List<Integer> jobDuration = new ArrayList<>();
        List<List<Integer>> jobResource = new ArrayList<>();

        int numberJob = Integer.parseInt(data.get(5)[4]);
        int horizon = Integer.parseInt(data.get(6)[2]);
        List<Integer> resourceCapacity = Arrays.stream(data.get(89))
                                             .map(Integer::parseInt)
                                             .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        System.out.println(resourceCapacity);
        // Process successors
        for (int job = 18; job < 18 + numberJob; job++) {
            jobNumSuccessors.add(Integer.parseInt(data.get(job)[2]));
            List<Integer> successors = new ArrayList<>();
            for (int i = 3; i < data.get(job).length; i++) {
                successors.add(Integer.parseInt(data.get(job)[i]));
            }
            jobSuccessors.add(successors);
        }

        // Process predecessors
        for (int i = 0; i < numberJob; i++) {
            jobPredecessors.add(new ArrayList<>());
        }
        for (int job = 0; job < numberJob; job++) {
            for (int successor : jobSuccessors.get(job)) {
                jobPredecessors.get(successor - 1).add(job + 1);
            }
        }

        // Process duration and resources
        int row = 54;
        for (int job = 0; job < numberJob; job++) {
            jobDuration.add(Integer.parseInt(data.get(row)[2]));
            List<Integer> resources = new ArrayList<>();
            for (int i = 3; i < 7; i++) {
                resources.add(Integer.parseInt(data.get(row)[i]));
            }
            jobResource.add(resources);
            row++;
        }

        return new JobData(numberJob, horizon, jobNumSuccessors, jobSuccessors,
                         jobPredecessors, jobDuration, jobResource, resourceCapacity);
    }
}