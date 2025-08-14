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
        JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);
        // Convert JobDataInstance to Project format
        this.project = convertToProject(noDummyData);
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
        // Convert List<Integer>[] to List<Integer>[]
        @SuppressWarnings("unchecked")
        List<Integer>[] P = (List<Integer>[]) new List[data.numberJob + 2];
        @SuppressWarnings("unchecked")
        List<Integer>[] S = (List<Integer>[]) new List[data.numberJob + 2];
        
        for (int i = 0; i <= data.numberJob + 1; i++) {
            P[i] = new ArrayList<>();
            S[i] = new ArrayList<>();
            
            if (i < data.jobPredecessors.size() && data.jobPredecessors.get(i) != null) {
                for (Integer pred : data.jobPredecessors.get(i)) {
                    P[i].add(pred);
                }
            }
            
            if (i < data.jobSuccessors.size() && data.jobSuccessors.get(i) != null) {
                for (Integer succ : data.jobSuccessors.get(i)) {
                    S[i].add(succ);
                }
            }
        }
        
        // Convert durations to array
        int[] durations = data.jobDuration.stream().mapToInt(Integer::intValue).toArray();
        
        // Convert resource requirements to 2D array
        int[][] resources = new int[data.jobResource.size()][];
        for (int i = 0; i < data.jobResource.size(); i++) {
            resources[i] = data.jobResource.get(i).stream().mapToInt(Integer::intValue).toArray();
        }
        
        return new Project(
            data.numberJob,
            data.resourceCapacity.size(),
            durations,
            resources,
            data.resourceCapacity.stream().mapToInt(Integer::intValue).toArray(),
            P,
            S,
            data.horizon
        );
    }
}