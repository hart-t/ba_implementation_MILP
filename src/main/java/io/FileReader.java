package io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileReader {

    public JobDataInstance dataRead(String file) throws Exception {
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

        String instanceName = file.substring(file.lastIndexOf('/') + 1, file.lastIndexOf('.'));

        return new JobDataInstance(instanceName, numberJob, horizon, jobNumSuccessors, jobSuccessors,
                         jobPredecessors, jobDuration, jobResource, resourceCapacity);
    }
    /*
     * This method is not used in the current implementation, but can be used to read results from a file.
     * It is commented out to avoid confusion, but can be uncommented if needed.
     * It reads a file containing solver results and returns a SolverResults object.
     *
    /*
     * public SolverResults readResults(String file) throws Exception {
        Double upperBound = null;
        Double lowerBound = null;
        Double objectiveValue = null;
        Double timeInSeconds = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Upper bound:")) {
                    upperBound = Double.parseDouble(line.split(":")[1].trim());
                } else if (line.startsWith("Lower bound:")) {
                    lowerBound = Double.parseDouble(line.split(":")[1].trim());
                } else if (line.startsWith("Objective value:")) {
                    objectiveValue = Double.parseDouble(line.split(":")[1].trim());
                } else if (line.startsWith("Time in seconds:")) {
                    timeInSeconds = Double.parseDouble(line.split(":")[1].trim());
                }
            }
        }

        return new SolverResults(upperBound, lowerBound, objectiveValue, timeInSeconds);
    }
     */
}