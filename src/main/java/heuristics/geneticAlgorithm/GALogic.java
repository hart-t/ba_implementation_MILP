package heuristics.geneticAlgorithm;

import io.JobDataInstance;
import utility.DeleteDummyJobs;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;

public class GALogic {
    Project project;
    ProjectPreprocessor preprocessor;
    SSGSDecoder decoder;
    ActivityListHelper listHelper;
    PopulationInitializer populationInitializer;
    GeneticOperators geneticOperators;
    HartmannGA hartmannGA;

    public GALogic(JobDataInstance data) {
        // Convert JobDataInstance to Project format
        this.project = convertToProject(data);
        this.preprocessor = project.getPreprocessor();
        this.decoder = new SSGSDecoder(project);
        this.listHelper = new ActivityListHelper(project);
        this.populationInitializer = new PopulationInitializer(project);
        this.geneticOperators = new GeneticOperators(project);
        this.hartmannGA = new HartmannGA(project);
    }
    
    public GALogic(Project project) {
        this.project = project;
        this.preprocessor = project.getPreprocessor();
        this.decoder = new SSGSDecoder(project);
        this.listHelper = new ActivityListHelper(project);
        this.populationInitializer = new PopulationInitializer(project);
        this.geneticOperators = new GeneticOperators(project);
        this.hartmannGA = new HartmannGA(project);
    }
    
    /**
     * Decode an activity list to get makespan and schedule
     */
    public Individual decodeActivityList(int[] activityList) {
        return decoder.decode(activityList);
    }
    
    /**
     * Generate a random feasible individual
     */
    public Individual generateRandomIndividual(java.util.Random random) {
        int[] activityList = listHelper.generateRandomFeasibleList(random);
        return decoder.decode(activityList);
    }
    
    /**
     * Repair an individual with infeasible activity list
     */
    public Individual repairIndividual(Individual individual) {
        int[] repairedList = listHelper.repairActivityList(individual.activityList);
        return decoder.decode(repairedList);
    }
    
    /**
     * Generate initial population with optional heuristic seeding
     */
    public java.util.List<Individual> generateInitialPopulation(int populationSize, java.util.Random random, 
                                                    java.util.Map<Integer, Integer> heuristicSchedule) {
        return populationInitializer.generateInitialPopulation(populationSize, random, heuristicSchedule);
    }
    
    /**
     * Generate initial population using only RBRS
     */
    public java.util.List<Individual> generateInitialPopulation(int populationSize, java.util.Random random) {
        return populationInitializer.generateInitialPopulation(populationSize, random);
    }
    
    /**
     * Run genetic algorithm
     */
    public Individual runGeneticAlgorithm(int populationSize, int maxGenerations, 
                                        double mutationRate, Random random,
                                        Map<Integer, Integer> heuristicSchedule) {
        // Initialize population
        List<Individual> population = generateInitialPopulation(populationSize, random, heuristicSchedule);
        
        Individual bestOverall = geneticOperators.getBestIndividual(population);
        
        // Evolution loop
        for (int generation = 0; generation < maxGenerations; generation++) {
            // Create next generation
            population = geneticOperators.createNextGeneration(population, mutationRate, random);
            
            // Track best individual
            Individual currentBest = geneticOperators.getBestIndividual(population);
            if (currentBest.makespan < bestOverall.makespan) {
                bestOverall = currentBest;
            }
            
            // Optional: print progress (debug)
            if (generation % 100 == 0) {
                double diversity = geneticOperators.calculateDiversity(population);
                System.out.printf("Generation %d: Best = %d, Diversity = %.2f%n", 
                                generation, currentBest.makespan, diversity);
            }
        }
        
        return bestOverall;
    }
    
    /**
     * Run with default parameters
     */
    public Individual runHartmannGA() {
        return hartmannGA.run();
    }
    
    /**
     * Run with time limit
     */
    public Individual runHartmannGA(long timeLimitMillis) {
        return hartmannGA.run(timeLimitMillis);
    }
    
    /**
     * Run with heuristic seed
     */
    public Individual runHartmannGA(Map<Integer, Integer> heuristicSchedule) {
        return hartmannGA.run(heuristicSchedule, 0);
    }
    
    /**
     * Run with heuristic seed and time limit
     */
    public Individual runHartmannGA(Map<Integer, Integer> heuristicSchedule, long timeLimitMillis) {
        return hartmannGA.run(heuristicSchedule, timeLimitMillis);
    }
    
    private Project convertToProject(JobDataInstance data) {
        // The genetic algorithm actually EXPECTS dummy jobs in the Project format
        // Project.J = number of real jobs, but arrays include dummies at indices 0 and J+1
        
        int realJobs = data.numberJob - 2;  // 30 real jobs (exclude original dummies)
        int totalJobs = realJobs + 2;       // 32 total (30 real + 2 new dummies)
        
        // Create P, S arrays for ALL jobs (including new dummies at 0 and realJobs+1)
        @SuppressWarnings("unchecked")
        List<Integer>[] P = (List<Integer>[]) new List[totalJobs];
        @SuppressWarnings("unchecked")
        List<Integer>[] S = (List<Integer>[]) new List[totalJobs];
        
        // Initialize all lists
        for (int i = 0; i < totalJobs; i++) {
            P[i] = new ArrayList<>();
            S[i] = new ArrayList<>();
        }
        
        // Remove original dummy jobs and get clean data
        JobDataInstance cleanData = DeleteDummyJobs.deleteDummyJobs(data);
        
        // Copy clean relationships and shift indices by +1 (to make room for dummy start at 0)
        for (int i = 0; i < cleanData.numberJob; i++) {
            int projectIndex = i + 1; // Jobs 1-30 in project format
            
            // Copy predecessors
            if (i < cleanData.jobPredecessors.size() && cleanData.jobPredecessors.get(i) != null) {
                for (Integer pred : cleanData.jobPredecessors.get(i)) {
                    int predProjectIndex = pred + 1; // Shift by +1
                    if (predProjectIndex >= 1 && predProjectIndex <= realJobs) {
                        P[projectIndex].add(predProjectIndex);
                    }
                }
            }
            
            // Copy successors
            if (i < cleanData.jobSuccessors.size() && cleanData.jobSuccessors.get(i) != null) {
                for (Integer succ : cleanData.jobSuccessors.get(i)) {
                    int succProjectIndex = succ + 1; // Shift by +1
                    if (succProjectIndex >= 1 && succProjectIndex <= realJobs) {
                        S[projectIndex].add(succProjectIndex);
                    }
                }
            }
        }
        
        // Add dummy start job (index 0) - connects to all jobs without predecessors
        for (int i = 1; i <= realJobs; i++) {
            if (P[i].isEmpty()) {
                P[i].add(0);
                S[0].add(i);
            }
        }
        
        // Add dummy end job (index realJobs+1 = 31) - all jobs without successors connect to it
        for (int i = 1; i <= realJobs; i++) {
            if (S[i].isEmpty()) {
                S[i].add(realJobs + 1);
                P[realJobs + 1].add(i);
            }
        }
        
        // Create duration array: [dummy=0, real_jobs_1_to_30, dummy=0]
        int[] durations = new int[totalJobs];
        durations[0] = 0; // Dummy start
        for (int i = 0; i < cleanData.jobDuration.size(); i++) {
            durations[i + 1] = cleanData.jobDuration.get(i);
        }
        durations[realJobs + 1] = 0; // Dummy end
        
        // Create resource array: [dummy=0s, real_jobs_1_to_30, dummy=0s]
        int[][] resources = new int[totalJobs][cleanData.resourceCapacity.size()];
        // durations[0] and durations[realJobs+1] are already 0 (dummy jobs use no resources)
        for (int i = 0; i < cleanData.jobResource.size(); i++) {
            for (int k = 0; k < cleanData.resourceCapacity.size(); k++) {
                resources[i + 1][k] = cleanData.jobResource.get(i).get(k);
            }
        }
        
        // Resource capacity array
        int[] resourceCapacity = cleanData.resourceCapacity.stream().mapToInt(Integer::intValue).toArray();
        
        return new Project(
            realJobs,           // J = 30 (real jobs only, dummies not counted)
            cleanData.resourceCapacity.size(), // K = 4 (number of resources)
            durations,          // p[0..31] (includes dummies at 0 and 31)
            resources,          // r[0..31][0..3] (includes dummies at 0 and 31)
            resourceCapacity,   // R[0..3]
            P,                  // P[0..31] (includes dummies at 0 and 31)
            S,                  // S[0..31] (includes dummies at 0 and 31)
            cleanData.horizon   // horizon
        );
    }
}