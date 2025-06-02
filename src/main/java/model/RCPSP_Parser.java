package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RCPSP_Parser {

    public static class RCPSPInstance {
        public int numberOfJobs;
        public int horizon;
        public int numberOfResources;
        public int[] jobDurations;
        public int[][] jobSuccessors;
        public int[][] resourceRequirements;
        public int[] resourceAvailabilities;

        public int getNumberOfJobs() {
            return numberOfJobs;
        }
    }

    @org.jetbrains.annotations.NotNull
    public static RCPSPInstance parseFile(String filePath) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filePath));
        RCPSPInstance instance = new RCPSPInstance();

        // Skip to the relevant line containing job and resource count
        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.equals("):")) {
                instance.numberOfJobs = scanner.nextInt();
                System.out.println("Number of jobs: " + instance.numberOfJobs);
            } else if (token.equals("horizon")) {
                    scanner.next(); // skip ':'
                    instance.horizon = scanner.nextInt();
                    System.out.println("Horizon: " + instance.horizon);
            } else if (token.equals("renewable")) {
                scanner.next(); // skip ':'
                instance.numberOfResources = scanner.nextInt();
                System.out.println("Number of resources: " + instance.numberOfResources);
                break;
            }
        }

        instance.jobDurations = new int[instance.numberOfJobs];
        instance.jobSuccessors = new int[instance.numberOfJobs][];
        instance.resourceRequirements = new int[instance.numberOfJobs][instance.numberOfResources];

        // Skip lines to where job definitions start (find "PRECEDENCE RELATIONS:")
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().trim().startsWith("PRECEDENCE RELATIONS:")) break;
        }

        scanner.nextLine();
        //System.out.println(scanner.next());
        // Read job successors


        // Creating a matrix with the number of successors followed by the successor indices
        int[][] successorsMatrix = new int[instance.numberOfJobs][];

        for (int i = 0; i < instance.numberOfJobs; i++) {
            scanner.nextInt(); // job number
            scanner.nextInt(); // number of modes
            int numSuccessors = scanner.nextInt();
            //int numSuccessors = instance.jobSuccessors[i].length;
            successorsMatrix[i] = new int[numSuccessors]; // First element for count, rest for indices.
            //successorsMatrix[i][0] = numSuccessors; // Number of successors
            for (int j = 0; j < numSuccessors; j++) {
                successorsMatrix[i][j] = scanner.nextInt();
            }
        }

        instance.jobSuccessors = successorsMatrix;

        // Print matrix

        for (int i = 0; i < successorsMatrix.length; i++) {
            System.out.print("Row " + i + ": ");
            for (int j = 0; j < successorsMatrix[i].length; j++) {
                System.out.print(successorsMatrix[i][j] + " ");
            }
            System.out.println();
        }

        // Skip lines to where durations and demands start ("REQUESTS/DURATIONS:")
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().trim().startsWith("REQUESTS/DURATIONS:")) break;
        }

        // Skip header line
        scanner.nextLine();
        scanner.nextLine();

        // Read durations and resource requirements
        for (int i = 0; i < instance.numberOfJobs; i++) {
            scanner.nextInt(); // job number
            scanner.nextInt(); // mode
            instance.jobDurations[i] = scanner.nextInt(); // duration
            //System.out.println(instance.jobDurations[i]);
            for (int j = 0; j < instance.numberOfResources; j++) {
                instance.resourceRequirements[i][j] = scanner.nextInt();
                //System.out.println(instance.resourceRequirements[i][j]);
            }
        }

        // Print matrix

        for (int i = 0; i < instance.resourceRequirements.length; i++) {
            System.out.print("Row " + i + ": ");
            for (int j = 0; j < instance.resourceRequirements[i].length; j++) {
                System.out.print(instance.resourceRequirements[i][j] + " ");
            }
            System.out.println();
        }

        // Skip lines to "RESOURCEAVAILABILITIES"
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().trim().startsWith("RESOURCEAVAILABILITIES"))
                break;
        }

        scanner.nextLine(); // skip R1-4

        instance.resourceAvailabilities = new int[instance.numberOfResources];
        for (int i = 0; i < instance.numberOfResources; i++) {
            instance.resourceAvailabilities[i] = scanner.nextInt();
        }

        scanner.close();
        return instance;
    }
}