package heuristics.geneticAlgorithm;

import java.util.*;

public class GeneticOperators {
    private final Project project;
    private final ActivityListHelper listHelper;
    private final SSGSDecoder decoder;
    
    public GeneticOperators(Project project) {
        this.project = project;
        this.listHelper = new ActivityListHelper(project);
        this.decoder = new SSGSDecoder(project);
    }
    
    /**
     * Two-point crossover ensuring activity lists exclude dummies
     */
    public Individual[] twoPointCrossover(Individual m, Individual f) {
        int J = m.activityList.length; // Should be project.J (excludes dummies)
        int[] M = m.activityList;
        int[] F = f.activityList;
        
        if (J <= 1) {
            // Handle edge case of very small problems
            return new Individual[]{new Individual(M.clone()), new Individual(F.clone())};
        }
        
        // Draw crossover points
        Random rng = new Random(); 
        int q1 = rng.nextInt(J - 1); // 0 to J-2
        int q2 = rng.nextInt(J - q1 - 1) + q1 + 1; // q1+1 to J-1
        
        int[] daughter = createOffspringOptimized(M, F, q1, q2);
        int[] son = createOffspringOptimized(F, M, q1, q2);
        
        return new Individual[]{new Individual(daughter), new Individual(son)};
    }
    
    private int[] createOffspringOptimized(int[] primary, int[] secondary, int q1, int q2) {
        int J = project.J;
        int[] offspring = new int[J];
        boolean[] used = new boolean[J + 2];
        
        // Phase 1: Copy positions 0..q1-1 from primary parent
        for (int i = 0; i < q1; i++) {
            offspring[i] = primary[i];
            used[primary[i]] = true;
        }
        
        // Phase 2: Fill positions q1..q2-1 with unused genes from secondary in secondary-order
        int fillIndex = q1;
        for (int i = 0; i < J && fillIndex < q2; i++) {
            int gene = secondary[i];
            if (!used[gene]) {
                offspring[fillIndex] = gene;
                used[gene] = true;
                fillIndex++;
            }
        }
        
        // Phase 3: Fill positions q2..J-1 from primary parent (skipping already used)
        int primaryIndex = 0;
        for (int i = q2; i < J; i++) {
            // Find next unused gene in primary parent
            while (primaryIndex < J && used[primary[primaryIndex]]) {
                primaryIndex++;
            }
            if (primaryIndex < J) {
                offspring[i] = primary[primaryIndex];
                used[primary[primaryIndex]] = true;
                primaryIndex++;
            }
        }
        
        return offspring;
    }
    
    /**
     * Mutate individual in place - skip swaps that violate precedence (don't repair)
     */
    public void mutateInPlace(Individual x, double PMUT, Random rng) {
        int[] list = x.activityList;
        int J = list.length; // Activity list excludes dummy 0 and J+1
        
        // For i=0..J-2: if rng.nextDouble()<PMUT and canSwapAdjacent then swap
        for (int i = 0; i < J - 1; i++) {
            if (rng.nextDouble() < PMUT) {
                // Only swap if precedence-feasible, otherwise skip (don't repair)
                if (listHelper.canSwapAdjacent(list, i)) {
                    int temp = list[i];
                    list[i] = list[i + 1];
                    list[i + 1] = temp;
                }
            }
        }
        
        // Mark as needing re-decoding
        x.makespan = -1;
        x.start = null;
    }
    
    /**
     * Ranking selection - keep best POP individuals from doubled population
     */
    public List<Individual> rankingSelection(List<Individual> population, int targetSize) {
        // Decode all individuals that haven't been decoded yet
        for (Individual individual : population) {
            if (!individual.isDecoded()) {
                Individual decoded = decoder.decode(individual.activityList);
                individual.makespan = decoded.makespan;
                individual.start = decoded.start;
            }
        }
        
        // Sort by makespan (ascending - lower is better)
        population.sort(Comparator.comparingInt(ind -> ind.makespan));
        
        // Keep only the best targetSize individuals
        return new ArrayList<>(population.subList(0, Math.min(targetSize, population.size())));
    }
    
    /**
     * Tournament selection for parent selection
     */
    public Individual tournamentSelection(List<Individual> population, int tournamentSize, Random random) {
        Individual best = null;
        
        for (int i = 0; i < tournamentSize; i++) {
            Individual candidate = population.get(random.nextInt(population.size()));
            
            // Ensure candidate is decoded
            if (!candidate.isDecoded()) {
                Individual decoded = decoder.decode(candidate.activityList);
                candidate.makespan = decoded.makespan;
                candidate.start = decoded.start;
            }
            
            if (best == null || candidate.makespan < best.makespan) {
                best = candidate;
            }
        }
        
        return best;
    }
    
    /**
     * Create next generation using crossover and mutation
     */
    public List<Individual> createNextGeneration(List<Individual> currentPopulation, 
                                                double mutationRate, Random random) {
        List<Individual> newGeneration = new ArrayList<>(currentPopulation);
        int populationSize = currentPopulation.size();
        
        // Create offspring through crossover
        for (int i = 0; i < populationSize / 2; i++) {
            // Select parents (can use tournament or random selection)
            Individual mother = tournamentSelection(currentPopulation, 3, random);
            Individual father = tournamentSelection(currentPopulation, 3, random);
            
            // Perform crossover
            Individual[] offspring = twoPointCrossover(mother, father);
            
            // Mutate offspring
            mutateInPlace(offspring[0], mutationRate, random);
            mutateInPlace(offspring[1], mutationRate, random);
            
            // Add to new generation
            newGeneration.add(offspring[0]);
            newGeneration.add(offspring[1]);
        }
        
        // Apply ranking selection to keep best individuals
        return rankingSelection(newGeneration, populationSize);
    }
    
    /**
     * Get best individual from population
     */
    public Individual getBestIndividual(List<Individual> population) {
        Individual best = null;
        
        for (Individual individual : population) {
            // Ensure individual is decoded
            if (!individual.isDecoded()) {
                Individual decoded = decoder.decode(individual.activityList);
                individual.makespan = decoded.makespan;
                individual.start = decoded.start;
            }
            
            if (best == null || individual.makespan < best.makespan) {
                best = individual;
            }
        }
        
        return best;
    }
    
    /**
     * Calculate population diversity (average hamming distance)
     */
    public double calculateDiversity(List<Individual> population) {
        if (population.size() < 2) return 0.0;
        
        double totalDistance = 0.0;
        int comparisons = 0;
        
        for (int i = 0; i < population.size(); i++) {
            for (int j = i + 1; j < population.size(); j++) {
                totalDistance += hammingDistance(population.get(i).activityList, 
                                               population.get(j).activityList);
                comparisons++;
            }
        }
        
        return totalDistance / comparisons;
    }
    
    private int hammingDistance(int[] list1, int[] list2) {
        int distance = 0;
        int minLength = Math.min(list1.length, list2.length);
        
        for (int i = 0; i < minLength; i++) {
            if (list1[i] != list2[i]) {
                distance++;
            }
        }
        
        return distance + Math.abs(list1.length - list2.length);
    }
}
