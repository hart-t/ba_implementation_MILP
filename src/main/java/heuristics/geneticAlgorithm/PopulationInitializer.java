package heuristics.geneticAlgorithm;

import java.util.*;

public class PopulationInitializer {
    private final Project project;
    private final ProjectPreprocessor preprocessor;
    private final ActivityListHelper listHelper;
    private final SSGSDecoder decoder;
    private final int[] lft; // Latest Finish Time for each job
    
    public PopulationInitializer(Project project) {
        this.project = project;
        this.preprocessor = project.getPreprocessor();
        this.listHelper = new ActivityListHelper(project);
        this.decoder = new SSGSDecoder(project);
        this.lft = computeLatestFinishTimes();
    }
    
    /**
     * Compute Latest Finish Times (LFT) for each job
     */
    private int[] computeLatestFinishTimes() {
        int[] lft = new int[project.J + 2];
        boolean[] computed = new boolean[project.J + 2];
        
        // Process in reverse topological order
        int[] topOrder = preprocessor.topologicalOrder;
        for (int idx = topOrder.length - 1; idx >= 0; idx--) {
            int j = topOrder[idx];
            computeLFT(j, lft, computed);
        }
        
        return lft;
    }
    
    private void computeLFT(int j, int[] lft, boolean[] computed) {
        if (computed[j]) return;
        
        // Base case: dummy end job
        if (j == project.J + 1) {
            lft[j] = project.horizon;
            computed[j] = true;
            return;
        }
        
        // LFT[j] = min_{k∈S[j]} (LFT[k]) - p[j]
        int minSuccessorLFT = project.horizon;
        
        for (int i = 0; i < project.S[j].size(); i++) {
            int succ = project.S[j].get(i);
            if (!computed[succ]) {
                computeLFT(succ, lft, computed);
            }
            minSuccessorLFT = Math.min(minSuccessorLFT, lft[succ]);
        }
        
        lft[j] = minSuccessorLFT - project.p[j];
        computed[j] = true;
    }
    
    /**
     * Generate activity list using RBRS with LFT priority
     */
    public int[] generateActivityListRBRS(Random random) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> eligible = new HashSet<>();
        int[] remainingPreds = new int[project.J + 2];
        
        // Initialize eligible set and predecessor counts
        for (int j = 1; j <= project.J; j++) {
            remainingPreds[j] = 0;
            for (int i = 0; i < project.P[j].size(); i++) {
                int pred = project.P[j].get(i);
                if (pred != 0) { // Exclude dummy start
                    remainingPreds[j]++;
                }
            }
            
            if (remainingPreds[j] == 0) {
                eligible.add(j);
            }
        }
        
        // Build activity list using RBRS
        while (!eligible.isEmpty()) {
            int selected = selectJobRBRS(eligible, random);
            result.add(selected);
            eligible.remove(selected);
            
            // Update eligible set
            for (int i = 0; i < project.S[selected].size(); i++) {
                int succ = project.S[selected].get(i);
                if (succ != project.J + 1) { // Exclude dummy end
                    remainingPreds[succ]--;
                    if (remainingPreds[succ] == 0) {
                        eligible.add(succ);
                    }
                }
            }
        }
        
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Select job using regret-based biased random sampling
     */
    private int selectJobRBRS(Set<Integer> eligible, Random random) {
        if (eligible.size() == 1) {
            return eligible.iterator().next();
        }
        
        // Find maximum LFT value in eligible set
        int maxLFT = Integer.MIN_VALUE;
        for (int j : eligible) {
            maxLFT = Math.max(maxLFT, lft[j]);
        }
        
        // Compute regrets and selection probabilities
        double[] probabilities = new double[eligible.size()];
        int[] jobs = eligible.stream().mapToInt(Integer::intValue).toArray();
        double totalWeight = 0.0;
        
        for (int i = 0; i < jobs.length; i++) {
            int j = jobs[i];
            int regret = maxLFT - lft[j]; // Higher LFT is better, so regret = max - current
            double weight = regret + 1.0; // Add 1 to avoid zero weights
            probabilities[i] = weight;
            totalWeight += weight;
        }
        
        // Normalize probabilities
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= totalWeight;
        }
        
        // Sample according to probabilities
        double r = random.nextDouble();
        double cumulative = 0.0;
        
        for (int i = 0; i < jobs.length; i++) {
            cumulative += probabilities[i];
            if (r <= cumulative) {
                return jobs[i];
            }
        }
        
        // Fallback (should not happen)
        return jobs[jobs.length - 1];
    }
    
    /**
     * Convert heuristic schedule (job -> start time) to activity list
     */
    public int[] convertHeuristicToActivityList(Map<Integer, Integer> jobStartTimes) {
        List<JobStartPair> pairs = new ArrayList<>();
        
        for (int j = 1; j <= project.J; j++) {
            int startTime = jobStartTimes.getOrDefault(j, 0);
            int topOrderIndex = getTopologicalOrderIndex(j);
            pairs.add(new JobStartPair(j, startTime, topOrderIndex));
        }
        
        // Sort by start time, then by topological order for ties
        pairs.sort((a, b) -> {
            int cmp = Integer.compare(a.startTime, b.startTime);
            if (cmp == 0) {
                return Integer.compare(a.topOrderIndex, b.topOrderIndex);
            }
            return cmp;
        });
        
        return pairs.stream().mapToInt(pair -> pair.job).toArray();
    }
    
    private int getTopologicalOrderIndex(int job) {
        int[] topOrder = preprocessor.topologicalOrder;
        for (int i = 0; i < topOrder.length; i++) {
            if (topOrder[i] == job) {
                return i;
            }
        }
        return Integer.MAX_VALUE; // Should not happen
    }
    
    /**
     * Generate initial population with mix of RBRS and heuristic seeding
     */
    public List<Individual> generateInitialPopulation(int populationSize, Random random, 
                                                    Map<Integer, Integer> heuristicSchedule) {
        List<Individual> population = new ArrayList<>();
        
        // Add heuristic seed if provided
        if (heuristicSchedule != null && !heuristicSchedule.isEmpty()) {
            int[] heuristicList = convertHeuristicToActivityList(heuristicSchedule);
            if (listHelper.isPrecedenceFeasible(heuristicList)) {
                Individual heuristicIndividual = decoder.decode(heuristicList);
                population.add(heuristicIndividual);
            }
        }
        
        // Fill remaining population with RBRS
        while (population.size() < populationSize) {
            int[] activityList = generateActivityListRBRS(random);
            Individual individual = decoder.decode(activityList);
            population.add(individual);
        }
        
        return population;
    }
    
    /**
     * Generate initial population using only RBRS
     */
    public List<Individual> generateInitialPopulation(int populationSize, Random random) {
        return generateInitialPopulation(populationSize, random, null);
    }
    
    /**
     * Initialize population using exact RBRS(LFT) specification
     */
    public List<Individual> initPopulation(int POP, Random rng, Map<Integer, Integer> heuristicSeed) {
        List<Individual> population = new ArrayList<>();
        
        // Seed injection: Convert {job → startTime} to activity list
        if (heuristicSeed != null && !heuristicSeed.isEmpty()) {
            Individual seedIndividual = createSeedFromHeuristic(heuristicSeed);
            if (seedIndividual != null) {
                population.add(seedIndividual);
            }
        }
        
        // Fill remaining population with RBRS(LFT)
        while (population.size() < POP) {
            int[] activityList = generateActivityListRBRSExact(rng);
            Individual individual = new Individual(activityList);
            population.add(individual);
        }
        
        return population;
    }
    
    /**
     * RBRS(LFT) exact implementation
     */
    private int[] generateActivityListRBRSExact(Random rng) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> eligible = new HashSet<>();
        int[] remainingPreds = new int[project.J + 2];
        
        // Initialize eligible set with successors of dummy 0 (exclude dummy end)
        for (int i = 0; i < project.S[0].size(); i++) {
            int succ = project.S[0].get(i);
            if (succ >= 1 && succ <= project.J) { // Only real jobs
                eligible.add(succ);
            }
        }
        
        // Initialize predecessor counts (exclude dummy start)
        for (int j = 1; j <= project.J; j++) {
            remainingPreds[j] = 0;
            for (int i = 0; i < project.P[j].size(); i++) {
                int pred = project.P[j].get(i);
                if (pred >= 1 && pred <= project.J) { // Only count real predecessors
                    remainingPreds[j]++;
                }
            }
        }
        
        // Build activity list (result contains only jobs 1..J)
        while (result.size() < project.J) {
            if (eligible.isEmpty()) {
                throw new RuntimeException("No eligible activities - precedence graph error");
            }
            
            int selectedJob = selectJobByLFTRegret(eligible, rng);
            result.add(selectedJob);
            eligible.remove(selectedJob);
            
            // Update eligible set
            for (int i = 0; i < project.S[selectedJob].size(); i++) {
                int succ = project.S[selectedJob].get(i);
                if (succ >= 1 && succ <= project.J) { // Only real jobs
                    remainingPreds[succ]--;
                    if (remainingPreds[succ] == 0) {
                        eligible.add(succ);
                    }
                }
            }
        }
        
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Select job by LFT regret-based sampling
     */
    private int selectJobByLFTRegret(Set<Integer> eligible, Random rng) {
        if (eligible.size() == 1) {
            return eligible.iterator().next();
        }
        
        // Compute LFT for each j ∈ eligible
        int maxLFT = Integer.MIN_VALUE;
        for (int j : eligible) {
            maxLFT = Math.max(maxLFT, lft[j]);
        }
        
        // Compute regrets ρ_j and sampling probabilities π_j = (ρ_j + 1) / Σ(ρ + 1)
        double[] probabilities = new double[eligible.size()];
        int[] jobs = eligible.stream().mapToInt(Integer::intValue).toArray();
        double totalWeight = 0.0;
        
        for (int i = 0; i < jobs.length; i++) {
            int j = jobs[i];
            int regret = maxLFT - lft[j]; // Higher LFT is better
            double weight = regret + 1.0;
            probabilities[i] = weight;
            totalWeight += weight;
        }
        
        // Normalize probabilities
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= totalWeight;
        }
        
        // Roulette wheel selection
        double r = rng.nextDouble();
        double cumulative = 0.0;
        
        for (int i = 0; i < jobs.length; i++) {
            cumulative += probabilities[i];
            if (r <= cumulative) {
                return jobs[i];
            }
        }
        
        // Fallback
        return jobs[jobs.length - 1];
    }
    
    /**
     * Create seed individual from heuristic schedule with proper edge case handling
     */
    private Individual createSeedFromHeuristic(Map<Integer, Integer> heuristicSchedule) {
        List<JobStartPair> pairs = new ArrayList<>();
        
        // Only include non-dummy jobs (1 to J)
        for (int j = 1; j <= project.J; j++) {
            int startTime = heuristicSchedule.getOrDefault(j, 0);
            int topoIndex = getTopologicalIndex(j);
            pairs.add(new JobStartPair(j, startTime, topoIndex));
        }
        
        // Sort by start time, ties broken by topological index
        // This automatically respects precedence since schedules are feasible
        pairs.sort((a, b) -> {
            int cmp = Integer.compare(a.startTime, b.startTime);
            if (cmp == 0) {
                return Integer.compare(a.topOrderIndex, b.topOrderIndex);
            }
            return cmp;
        });
        
        int[] activityList = pairs.stream().mapToInt(pair -> pair.job).toArray();
        
        // Verify precedence feasibility (should be true for feasible schedules)
        if (listHelper.isPrecedenceFeasible(activityList)) {
            return new Individual(activityList);
        } else {
            // Repair if necessary (edge case safety)
            int[] repairedList = listHelper.repairActivityList(activityList);
            return new Individual(repairedList);
        }
    }
    
    private int getTopologicalIndex(int job) {
        int[] topOrder = preprocessor.topologicalOrder;
        for (int i = 0; i < topOrder.length; i++) {
            if (topOrder[i] == job) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }
    
    /**
     * Helper class for sorting jobs by start time and topological order
     */
    private static class JobStartPair {
        final int job;
        final int startTime;
        final int topOrderIndex;
        
        JobStartPair(int job, int startTime, int topOrderIndex) {
            this.job = job;
            this.startTime = startTime;
            this.topOrderIndex = topOrderIndex;
        }
    }
}
