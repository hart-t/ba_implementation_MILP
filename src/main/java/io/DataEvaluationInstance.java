package io;

/*
 * 
 * Paramter	Instance	Model	H_M_Makespan	noH_M_Makespan	H_UB	H_LB	Optimal_Makespan	H_Time	noH_Time	Time_Diff	H_Makespan	time_limit_reached	Error	Heuristics
 */

public class DataEvaluationInstance {
    private String[] parameter;
    private String[] instance;
    private String[] modelType;
    private int[] hMakespan;
    private int[] noHMakespan;
    private int[] hUB;
    private int[] hLB;
    private int[] optimalMakespan;
    private double[] hTime;
    private double[] noHTime;
    private double[] timeDiff;
    private int[] heuristicMakespan;
    private boolean[] timeLimitReached;
    private boolean[] error;
    private String[] heuristics;

    public DataEvaluationInstance(String[] parameter, String[] instance, String[] modelType, int[] hMakespan, int[] noHMakespan, int[] hUB, int[] hLB, int[] optimalMakespan,
                                  double[] hTime, double[] noHTime, double[] timeDiff, int[] heuristicMakespan, boolean[] timeLimitReached, boolean[] error, String[] heuristics) {
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
    
    public int getDataCount() {
        return parameter != null ? parameter.length : 0;
    }
}
