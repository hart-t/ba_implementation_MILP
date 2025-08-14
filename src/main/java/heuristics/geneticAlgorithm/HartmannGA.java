package heuristics.geneticAlgorithm;

import java.util.*;

/*
 * Provides the complete genetic algorithm implementation that follows Hartmann's best configuration 
 * for J30 instances, managing the evolution process from initialization through termination.
 * 
 * 
 * 
 * 
 */

public class HartmannGA {
    private final Project project;
    private final int POP = 40;      // Population size (Hartmanns best for J30)
    private final int GEN = 25;      // Number of generations (1,000 total evaluations)
    private final double PMUT = 0.05; // Mutation rate (Hartmanns best)
    private final Random rng;
    
    // Components
    private final PopulationInitializer populationInitializer;
    private final GeneticOperators geneticOperators;
    private final SSGSDecoder decoder;
    
    public HartmannGA(Project project, Random rng) {
        this.project = project;
        this.rng = rng;
        this.populationInitializer = new PopulationInitializer(project);
        this.geneticOperators = new GeneticOperators(project);
        this.decoder = new SSGSDecoder(project);
    }
    
    public HartmannGA(Project project, long seed) {
        this(project, new Random(seed));
    }
    
    public HartmannGA(Project project) {
        this(project, new Random());
    }
    
    /**
     * Run the genetic algorithm with optional time limit
     */
    public Individual run(long timeLimitMillis) {
        long startTime = System.currentTimeMillis();
        
        // Initialize population using RBRS(LFT)
        List<Individual> pop = initPopulation();
        decodeAll(pop);
        Individual globalBest = best(pop);
        
        System.out.printf("Initial population: Best makespan = %d%n", globalBest.makespan);
        
        // Evolution loop
        for (int g = 0; g < GEN; g++) {
            List<Individual> children = new ArrayList<>(POP);
            
            // Pair up randomly for crossover
            Collections.shuffle(pop, rng);
            
            for (int i = 0; i < POP; i += 2) {
                Individual mom = pop.get(i);
                Individual dad = (i + 1 < POP) ? pop.get(i + 1) : pop.get(0); // Handle odd population
                
                // Two-point crossover
                Individual[] kids = geneticOperators.twoPointCrossover(mom, dad);
                
                // Mutation (adjacent swap if feasible)
                mutateInPlace(kids[0]);
                mutateInPlace(kids[1]);
                
                children.add(kids[0]);
                children.add(kids[1]);
            }
            
            // Decode all children
            decodeAll(children);
            
            // Combine populations (size = 2*POP)
            pop.addAll(children);
            
            // Ranking selection: sort and keep POP best
            pop.sort(Comparator.comparingInt(ind -> ind.makespan));
            pop = new ArrayList<>(pop.subList(0, POP));
            
            // Update global best
            if (pop.get(0).makespan < globalBest.makespan) {
                globalBest = clone(pop.get(0));
                System.out.printf("Generation %d: New best makespan = %d%n", g + 1, globalBest.makespan);
            }
            
            // Check time limit
            if (timeLimitMillis > 0 && System.currentTimeMillis() - startTime > timeLimitMillis) {
                System.out.printf("Time limit reached at generation %d%n", g + 1);
                break;
            }
        }
        
        System.out.printf("Final best makespan: %d%n", globalBest.makespan);
        return globalBest;
    }
    
    /**
     * Run without time limit
     */
    public Individual run() {
        return run(0); // 0 means no time limit
    }
    
    /**
     * Initialize population using exact RBRS(LFT)
     */
    private List<Individual> initPopulation() {
        return populationInitializer.initPopulation(POP, rng, null);
    }
    
    private List<Individual> initPopulation(Map<Integer, Integer> heuristicSeed) {
        return populationInitializer.initPopulation(POP, rng, heuristicSeed);
    }
    
    /**
     * Decode all individuals using exact specification
     */
    private void decodeAll(List<Individual> population) {
        for (Individual individual : population) {
            if (!individual.isDecoded()) {
                decoder.decode(individual, project);
            }
        }
    }
    
    /**
     * Mutate individual in place using exact specification
     */
    private void mutateInPlace(Individual individual) {
        geneticOperators.mutateInPlace(individual, PMUT, rng);
    }
    
    /**
     * Run with heuristic seed using exact initialization
     */
    public Individual run(Map<Integer, Integer> heuristicSchedule, long timeLimitMillis) {
        long startTime = System.currentTimeMillis();
        
        // Initialize population with heuristic seed (exact method)
        List<Individual> pop = initPopulation(heuristicSchedule);
        decodeAll(pop);
        Individual globalBest = best(pop);
        
        System.out.printf("Initial population (with seed): Best makespan = %d%n", globalBest.makespan);
        
        // Evolution loop with exact operators
        for (int g = 0; g < GEN; g++) {
            List<Individual> children = new ArrayList<>(POP);
            
            Collections.shuffle(pop, rng);
            
            for (int i = 0; i < POP; i += 2) {
                Individual mom = pop.get(i);
                Individual dad = (i + 1 < POP) ? pop.get(i + 1) : pop.get(0);
                
                // Exact two-point crossover
                Individual[] kids = geneticOperators.twoPointCrossover(mom, dad);
                
                // Exact mutation
                mutateInPlace(kids[0]);
                mutateInPlace(kids[1]);
                
                children.add(kids[0]);
                children.add(kids[1]);
            }
            
            // Decode all children
            decodeAll(children);
            
            // Combine and select (survival-of-the-fittest)
            pop.addAll(children);
            pop.sort(Comparator.comparingInt(ind -> ind.makespan));
            pop = new ArrayList<>(pop.subList(0, POP));
            
            // Update global best (elitism - always keep globalBest)
            if (pop.get(0).makespan < globalBest.makespan) {
                globalBest = clone(pop.get(0));
                System.out.printf("Generation %d: New best makespan = %d%n", g + 1, globalBest.makespan);
            }
            
            // Check time limit
            if (timeLimitMillis > 0 && System.currentTimeMillis() - startTime > timeLimitMillis) {
                System.out.printf("Time limit reached at generation %d%n", g + 1);
                break;
            }
        }
        
        System.out.printf("Final best makespan: %d%n", globalBest.makespan);
        return globalBest;
    }
    
    /**
     * Find best individual in population
     */
    private Individual best(List<Individual> population) {
        return population.stream()
                .min(Comparator.comparingInt(ind -> ind.makespan))
                .orElseThrow(() -> new RuntimeException("Empty population"));
    }
    
    /**
     * Clone an individual
     */
    private Individual clone(Individual individual) {
        return new Individual(
            individual.activityList.clone(),
            individual.makespan,
            individual.start != null ? individual.start.clone() : null
        );
    }
    
    /**
     * Get algorithm parameters
     */
    public GAParameters getParameters() {
        return new GAParameters(POP, GEN, PMUT);
    }
    
    /**
     * Record class for GA parameters
     */
    public static record GAParameters(int populationSize, int generations, double mutationRate) {
        @Override
        public String toString() {
            return String.format("GA Parameters: POP=%d, GEN=%d, PMUT=%.3f", 
                               populationSize, generations, mutationRate);
        }
    }
}
