package heuristics.geneticAlgorithm;

import java.util.Map;
import java.util.Random;

public final class RcpspHartmannGA {
    private final Project project;
    private final Random rng;
    
    // Configurable parameters
    private int populationSize = 40;
    private int generations = 25;
    private double mutationRate = 0.05;
    private long timeLimitMillis = 0; // 0 = no limit
    private Map<Integer, Integer> seedSchedule = null;
    
    public RcpspHartmannGA(Project project, long seed) {
        this.project = project;
        this.rng = new Random(seed);
    }
    
    public RcpspHartmannGA withPopulation(int pop) {
        this.populationSize = pop;
        return this;
    }
    
    public RcpspHartmannGA withGenerations(int gen) {
        this.generations = gen;
        return this;
    }
    
    public RcpspHartmannGA withMutation(double pmut) {
        this.mutationRate = pmut;
        return this;
    }
    
    public RcpspHartmannGA withTimeLimitMillis(long ms) {
        this.timeLimitMillis = ms;
        return this;
    }
    
    public RcpspHartmannGA seedWithSchedule(Map<Integer, Integer> startTimes) {
        this.seedSchedule = startTimes;
        return this;
    }
    
    /**
     * Solve and return best individual with activity list, start times, and makespan
     */
    public Individual solve() {
        HartmannGAConfigurable ga = new HartmannGAConfigurable(
            project, rng, populationSize, generations, mutationRate
        );
        
        if (seedSchedule != null) {
            return ga.run(seedSchedule, timeLimitMillis);
        } else {
            return ga.run(timeLimitMillis);
        }
    }
    
    /**
     * Configurable version of HartmannGA for the API
     */
    private static class HartmannGAConfigurable {
        private final Project project;
        private final Random rng;
        private final int POP;
        private final int GEN;
        private final double PMUT;
        
        // Components
        private final PopulationInitializer populationInitializer;
        private final GeneticOperators geneticOperators;
        private final SSGSDecoder decoder;
        
        public HartmannGAConfigurable(Project project, Random rng, int pop, int gen, double pmut) {
            this.project = project;
            this.rng = rng;
            this.POP = pop;
            this.GEN = gen;
            this.PMUT = pmut;
            
            this.populationInitializer = new PopulationInitializer(project);
            this.geneticOperators = new GeneticOperators(project);
            this.decoder = new SSGSDecoder(project);
        }
        
        public Individual run(long timeLimitMillis) {
            return run(null, timeLimitMillis);
        }
        
        public Individual run(Map<Integer, Integer> heuristicSchedule, long timeLimitMillis) {
            long startTime = System.currentTimeMillis();
            
            // Initialize population
            java.util.List<Individual> pop = populationInitializer.initPopulation(POP, rng, heuristicSchedule);
            decodeAll(pop);
            Individual globalBest = best(pop);
            
            // Evolution loop
            for (int g = 0; g < GEN; g++) {
                java.util.List<Individual> children = new java.util.ArrayList<>(POP);
                
                // Pair up randomly
                java.util.Collections.shuffle(pop, rng);
                for (int i = 0; i < POP; i += 2) {
                    Individual mom = pop.get(i);
                    Individual dad = (i + 1 < POP) ? pop.get(i + 1) : pop.get(0);
                    
                    Individual[] kids = geneticOperators.twoPointCrossover(mom, dad);
                    geneticOperators.mutateInPlace(kids[0], PMUT, rng);
                    geneticOperators.mutateInPlace(kids[1], PMUT, rng);
                    
                    children.add(kids[0]);
                    children.add(kids[1]);
                }
                
                // Decode, combine, and select
                decodeAll(children);
                pop.addAll(children);
                pop.sort(java.util.Comparator.comparingInt(ind -> ind.makespan));
                pop = new java.util.ArrayList<>(pop.subList(0, POP));
                
                // Update global best (elitism)
                if (pop.get(0).makespan < globalBest.makespan) {
                    globalBest = clone(pop.get(0));
                }
                
                // Check time limit
                if (timeLimitMillis > 0 && System.currentTimeMillis() - startTime > timeLimitMillis) {
                    break;
                }
            }
            
            return globalBest;
        }
        
        private void decodeAll(java.util.List<Individual> population) {
            for (Individual individual : population) {
                if (!individual.isDecoded()) {
                    decoder.decode(individual, project);
                }
            }
        }
        
        private Individual best(java.util.List<Individual> population) {
            return population.stream()
                    .min(java.util.Comparator.comparingInt(ind -> ind.makespan))
                    .orElseThrow(() -> new RuntimeException("Empty population"));
        }
        
        private Individual clone(Individual individual) {
            return new Individual(
                individual.activityList.clone(),
                individual.makespan,
                individual.start != null ? individual.start.clone() : null
            );
        }
    }
}
