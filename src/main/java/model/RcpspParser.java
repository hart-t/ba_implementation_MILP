package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RcpspParser {

    public static class dataInstance {

        public final int numberJob;
        public final int horizon;
        public final List<Integer> jobNumSuccessors;
        public final List<List<Integer>> jobSuccessors;
        public final List<List<Integer>> jobPredecessors;
        public final List<Integer> jobDuration;
        public final List<List<Integer>> jobResource;
        public final List<Integer> resourceCapacity;

        public dataInstance(int numberJob, int horizon, List<Integer> jobNumSuccessors,
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


    public dataInstance readFile(String filePath) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filePath));


        int numberJob = 0;
        int horizon = 0;
        int numberOfResources = 0;
        List<Integer> jobNumSuccessors = new ArrayList<>();
        List<List<Integer>> jobSuccessors = new ArrayList<>();
        List<List<Integer>> jobPredecessors = new ArrayList<>();
        List<Integer> jobDuration = new ArrayList<>();
        List<List<Integer>> jobResource = new ArrayList<>();
        List<Integer> resourceCapacity = new ArrayList<>();


        // Skip to the relevant line containing job and resource count
        label:
        while (scanner.hasNext()) {
            String token = scanner.next();
            switch (token) {
                case "):":
                    numberJob = scanner.nextInt();
                    System.out.println("Number of jobs: " + numberJob);
                    break;
                case "horizon":
                    scanner.next(); // skip ':'

                    horizon = scanner.nextInt();
                    System.out.println("Horizon: " + horizon);
                    break;
                case "renewable":
                    scanner.next(); // skip ':'

                    numberOfResources = scanner.nextInt();
                    System.out.println("Number of resources: " + numberOfResources);
                    break label;
            }
        }


        // Skip lines to where job definitions start (find "PRECEDENCE RELATIONS:")
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().trim().startsWith("PRECEDENCE RELATIONS:")) break;
        }

        scanner.nextLine();
        //System.out.println(scanner.next());



        // Process predecessors

        System.out.println(numberJob);

        for (int i = 0; i < numberJob; i++) {
            List<Integer> successors = new ArrayList<>();
            scanner.nextInt(); // job number
            scanner.nextInt(); // number of modes
            int numSuccessors = scanner.nextInt();
            for (int j = 0; j < numSuccessors; j++) {
                successors.add(scanner.nextInt());
            }
            jobSuccessors.add(successors);
        }

        for (int i = 0; i < numberJob; i++) {
            jobPredecessors.add(new ArrayList<>());
        }
        for (int job = 0; job < numberJob; job++) {
            for (int successor : jobSuccessors.get(job)) {
                jobPredecessors.get(successor - 1).add(job + 1);
            }
        }



        // Print matrix
        /*
        for (int i = 0; i < successorsMatrix.length; i++) {
            System.out.print("Row " + i + ": ");
            for (int j = 0; j < successorsMatrix[i].length; j++) {
                System.out.print(successorsMatrix[i][j] + " ");
            }
            System.out.println();
        }*/

        // Skip lines to where durations and demands start ("REQUESTS/DURATIONS:")
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().trim().startsWith("REQUESTS/DURATIONS:")) break;
        }

        // Skip header line
        scanner.nextLine();
        scanner.nextLine();

        // Read durations and resource requirements
        for (int job = 0; job < numberJob; job++) {
            scanner.nextInt(); // job number
            scanner.nextInt(); // mode
            jobDuration.add(scanner.nextInt()); // duration
            List<Integer> resources = new ArrayList<>();
            for (int i = 0; i < numberOfResources; i++) {
                resources.add(scanner.nextInt());
            }
            jobResource.add(resources);
        }

        // Print matrix
        /*
        for (int i = 0; i < instance.resourceRequirements.length; i++) {
            System.out.print("Row " + i + ": ");
            for (int j = 0; j < instance.resourceRequirements[i].length; j++) {
                System.out.print(instance.resourceRequirements[i][j] + " ");
            }
            System.out.println();
        }*/

        // Skip lines to "RESOURCEAVAILABILITIES"
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().trim().startsWith("RESOURCEAVAILABILITIES"))
                break;
        }

        scanner.nextLine(); // skip R1-4

        for (int i = 0; i < numberOfResources; i++) {
            resourceCapacity.add(scanner.nextInt());
        }

        scanner.close();
        return new dataInstance(numberJob, horizon, jobNumSuccessors, jobSuccessors,
                jobPredecessors, jobDuration, jobResource, resourceCapacity);
    }
}