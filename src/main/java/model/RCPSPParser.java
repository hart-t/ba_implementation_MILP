package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RCPSPParser {

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
        for (int i = 0; i < instance.numberOfJobs; i++) {
            scanner.nextInt(); // job number
            scanner.nextInt(); // number of modes
            int numSuccessors = scanner.nextInt();
            instance.jobSuccessors[i] = new int[numSuccessors];
            for (int j = 0; j < numSuccessors; j++) {
                instance.jobSuccessors[i][j] = scanner.nextInt();
            }
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
            for (int j = 0; j < instance.numberOfResources; j++) {
                instance.resourceRequirements[i][j] = scanner.nextInt();
            }
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
