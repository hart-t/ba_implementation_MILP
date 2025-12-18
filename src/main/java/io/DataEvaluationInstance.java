package io;

/*
 * 
 * Paramter	Instance	Model	H_M_Makespan	noH_M_Makespan	H_UB	H_LB	Optimal_Makespan	H_Time	noH_Time	Time_Diff	H_Makespan	time_limit_reached	Error	Heuristics
 */

public class DataEvaluationInstance {
    private String[] parameter;
    private String[] instance;
    private String[] modelType;
    private int[] hMakespan;                            // best computed makespan from the model with the use of a heuristic start solution
    private int[] noHMakespan;                          // best computed makespan from the model without the use of a heuristic start solution
    private int[] hUB;                                  // 
    private int[] hLB;                                  // lower bound on the makespan (with the use of a heuristic start solution)
    private int[] optimalMakespan;                      // optimal makespan
    private double[] hTime;                             // time it took to compute with the use of a start solution, computed with the use of start times from the heuristics
    private double[] noHTime;                           // time it took to compute without the use of a start solution
    private double[] timeDiff;                          // difference in time between heuristical and non-heuristical approaches
    private int[] heuristicMakespan;                    // best heuristical computed makespan
    private boolean[] timeLimitReached;                 // indicates if the time limit was reached
    private boolean[] error;                            // indicates if there was an error (for debungging only, delete later)
    private String[] heuristics;                        // heuristics used
    private String[] samplingMethod;                    // sampling method used
    private int[] samplingSize;                         // if sampling is used, it represents the number of tries for the start times

    public DataEvaluationInstance(String[] parameter, String[] instance, String[] modelType, int[] hMakespan, int[] noHMakespan, int[] hUB, int[] hLB, int[] optimalMakespan,
                                  double[] hTime, double[] noHTime, double[] timeDiff, int[] heuristicMakespan, boolean[] timeLimitReached, boolean[] error, String[] heuristics,
                                  String[] samplingMethod, int[] samplingSize) {
        this.parameter = parameter;
        this.instance = instance;
        this.modelType = modelType;
        this.hMakespan = hMakespan;
        this.noHMakespan = noHMakespan;
        this.hUB = hUB;
        this.hLB = hLB;
        this.optimalMakespan = optimalMakespan;
        this.hTime = hTime;
        this.noHTime = noHTime;
        this.timeDiff = timeDiff;
        this.heuristicMakespan = heuristicMakespan;
        this.timeLimitReached = timeLimitReached;
        this.error = error;
        this.heuristics = heuristics;
        this.samplingMethod = samplingMethod;
        this.samplingSize = samplingSize;
    }

    // Getter methods
    public String[] getParameter() { return parameter; }
    public String[] getInstance() { return instance; }
    public String[] getModelType() { return modelType; }
    public int[] getHMakespan() { return hMakespan; }
    public int[] getNoHMakespan() { return noHMakespan; }
    public int[] getHUB() { return hUB; }
    public int[] getHLB() { return hLB; }
    public int[] getOptimalMakespan() { return optimalMakespan; }
    public double[] getHTime() { return hTime; }
    public double[] getNoHTime() { return noHTime; }
    public double[] getTimeDiff() { return timeDiff; }
    public int[] getHeuristicMakespan() { return heuristicMakespan; }
    public boolean[] getTimeLimitReached() { return timeLimitReached; }
    public boolean[] getError() { return error; }
    public String[] getHeuristics() { return heuristics; }
    public String[] getSamplingMethod() { return samplingMethod; }
    public int[] getSamplingSize() { return samplingSize; }

    public int getDataCount() {
        return parameter != null ? parameter.length : 0;
    }
}
