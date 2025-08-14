package heuristics.geneticAlgorithm;

import io.JobDataInstance;
import java.util.Map;

public class GeneticAlgorithmRunner {
    
    /**
     * Run GA on JobDataInstance
     */
    public static Individual solve(JobDataInstance data) {
        GALogic gaLogic = new GALogic(data);
        return gaLogic.runHartmannGA();
    }
    
    /**
     * Run GA on JobDataInstance with time limit
     */
    public static Individual solve(JobDataInstance data, long timeLimitMillis) {
        GALogic gaLogic = new GALogic(data);
        return gaLogic.runHartmannGA(timeLimitMillis);
    }
    
    /**
     * Run GA on JobDataInstance with heuristic seed
     */
    public static Individual solve(JobDataInstance data, Map<Integer, Integer> heuristicSchedule) {
        GALogic gaLogic = new GALogic(data);
        return gaLogic.runHartmannGA(heuristicSchedule);
    }
    
    /**
     * Run GA on JobDataInstance with heuristic seed and time limit
     */
    public static Individual solve(JobDataInstance data, Map<Integer, Integer> heuristicSchedule, 
                                 long timeLimitMillis) {
        GALogic gaLogic = new GALogic(data);
        return gaLogic.runHartmannGA(heuristicSchedule, timeLimitMillis);
    }
    
    /**
     * Run GA on Project directly
     */
    public static Individual solve(Project project) {
        HartmannGA ga = new HartmannGA(project);
        return ga.run();
    }
    
    /**
     * Run GA on Project with parameters
     */
    public static Individual solve(Project project, Map<Integer, Integer> heuristicSchedule, 
                                 long timeLimitMillis) {
        HartmannGA ga = new HartmannGA(project);
        return ga.run(heuristicSchedule, timeLimitMillis);
    }
}
