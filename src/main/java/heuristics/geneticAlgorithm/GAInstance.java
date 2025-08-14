package heuristics.geneticAlgorithm;

import java.util.List;
import java.util.ArrayList;

public final class GAInstance {
    Project project;
    ProjectPreprocessor preprocessor;
    ActivityListHelper listHelper;
    PopulationInitializer populationInitializer;
    GeneticOperators geneticOperators;
    HartmannGA hartmannGA;

    public GAInstance(Project project) {
        this.project = project;
        this.preprocessor = project.getPreprocessor();
        this.listHelper = new ActivityListHelper(project);
        this.populationInitializer = new PopulationInitializer(project);
        this.geneticOperators = new GeneticOperators(project);
        this.hartmannGA = new HartmannGA(project);
    }
    
    /**
     * Solve using Hartmann's GA
     */
    public Individual solve() {
        return hartmannGA.run();
    }
    
    /**
     * Solve using Hartmann's GA with time limit
     */
    public Individual solve(long timeLimitMillis) {
        return hartmannGA.run(timeLimitMillis);
    }
    
    // Deprecated constructor for backwards compatibility
    @Deprecated
    public GAInstance(int nonDummyActivities, int numberOfResources, int[] p, int[][] r, int[] R, List<Integer>[] P, List<Integer>[] S, int horizon) {
        // Convert to new format
        @SuppressWarnings("unchecked")
        List<Integer>[] predecessors = (List<Integer>[]) new List[nonDummyActivities + 2];
        @SuppressWarnings("unchecked")
        List<Integer>[] successors = (List<Integer>[]) new List[nonDummyActivities + 2];
        
        for (int i = 0; i <= nonDummyActivities + 1; i++) {
            predecessors[i] = new ArrayList<>();
            successors[i] = new ArrayList<>();
            
            if (i < P.length && P[i] != null) {
                for (Integer pred : P[i]) {
                    predecessors[i].add(pred);
                }
            }
            
            if (i < S.length && S[i] != null) {
                for (Integer succ : S[i]) {
                    successors[i].add(succ);
                }
            }
        }
        
        this.project = new Project(nonDummyActivities, numberOfResources, p, r, R, predecessors, successors, horizon);
        this.preprocessor = project.getPreprocessor();
        this.listHelper = new ActivityListHelper(project);
        this.populationInitializer = new PopulationInitializer(project);
        this.geneticOperators = new GeneticOperators(project);
        this.hartmannGA = new HartmannGA(project);
    }
}