package io;

import heuristics.geneticAlgorithm.Project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class PSPLIBParser {
    
    public static Project parseFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            return parse(reader);
        }
    }

    private static Project parse(BufferedReader reader) throws IOException {
        String line;
        
        // Skip header lines until we find the project information
        while ((line = reader.readLine()) != null) {
            if (line.contains("PRECEDENCE RELATIONS:")) {
                break;
            }
        }
        
        // Read project parameters
        int jobs = 0, horizon = 0, resources = 4; // J30 standard
        
        // Find job count and horizon
        reader.mark(10000);
        while ((line = reader.readLine()) != null) {
            if (line.contains("jobs") && line.contains("horizon")) {
                String[] parts = line.trim().split("\\s+");
                jobs = Integer.parseInt(parts[0]);
                horizon = Integer.parseInt(parts[2]);
                break;
            }
        }
        reader.reset();
        
        // Initialize data structures
        int J = jobs - 2; // exclude dummy start (0) and end (J+1)
        int[] p = new int[J + 2]; // duration[0..J+1]
        int[][] r = new int[J + 2][resources]; // resource demand
        int[] R = new int[resources]; // resource availability
        @SuppressWarnings("unchecked")
        List<Integer>[] P = (List<Integer>[]) new List[J + 2]; // predecessors
        @SuppressWarnings("unchecked")
        List<Integer>[] S = (List<Integer>[]) new List[J + 2]; // successors
        
        for (int i = 0; i <= J + 1; i++) {
            P[i] = new ArrayList<>();
            S[i] = new ArrayList<>();
        }
        
        // Parse precedence relations
        while ((line = reader.readLine()) != null && !line.contains("REQUESTS/DURATIONS:")) {
            if (line.trim().isEmpty() || line.contains("jobnr.")) continue;
            
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 3) {
                int job = Integer.parseInt(parts[0]);
                int numSuccessors = Integer.parseInt(parts[2]);
                
                for (int i = 0; i < numSuccessors && i + 3 < parts.length; i++) {
                    int successor = Integer.parseInt(parts[3 + i]);
                    P[successor].add(job);
                    S[job].add(successor);
                }
            }
        }
        
        // Parse durations and resource requests
        while ((line = reader.readLine()) != null && !line.contains("RESOURCEAVAILABILITIES:")) {
            if (line.trim().isEmpty() || line.contains("jobnr.")) continue;
            
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 6) {
                int job = Integer.parseInt(parts[0]);
                p[job] = Integer.parseInt(parts[2]);
                
                for (int k = 0; k < resources && k + 3 < parts.length; k++) {
                    r[job][k] = Integer.parseInt(parts[3 + k]);
                }
            }
        }
        
        // Parse resource availabilities
        if ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            for (int k = 0; k < resources && k < parts.length; k++) {
                R[k] = Integer.parseInt(parts[k]);
            }
        }
        
        // Calculate horizon if not provided
        if (horizon <= 0) {
            horizon = Arrays.stream(p).sum();
        }
        
        return new Project(J, resources, p, r, R, P, S, horizon);
    }
}
