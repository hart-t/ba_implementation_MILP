package modelSolutions;

import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import interfaces.ModelSolutionInterface;
import enums.ModelType;

public class OnOffEventBasedModelSolution implements ModelSolutionInterface {
    GRBVar makespanVar;
    GRBVar[] startOfEventEVars;
    GRBVar[][] jobActiveAtEventVars;

    GRBModel model;
    int[][] earliestLatestStartTimes;
    long timeToCreateVariables;

    public OnOffEventBasedModelSolution(GRBVar makespanVar, GRBVar[] startOfEventE, GRBVar[][] jobActiveAtEvent, 
                                        GRBModel model, int[][] earliestLatestStartTimes, long timeToCreateVariables) {
        this.makespanVar = makespanVar;
        this.startOfEventEVars = startOfEventE;
        this.jobActiveAtEventVars = jobActiveAtEvent;
        this.earliestLatestStartTimes = earliestLatestStartTimes;
        this.model = model;
        this.timeToCreateVariables = timeToCreateVariables;
    }

    @Override
    public GRBModel getModel() {
        return model;
    }

    public int[][] getEarliestLatestStartTimes() {
        return earliestLatestStartTimes;
    }

    public GRBVar getMakespanVar() {
        return makespanVar;
    }

    public GRBVar[] getStartOfEventEVars() {
        return startOfEventEVars;
    }

    public GRBVar[][] getJobActiveAtEventVars() {
        return jobActiveAtEventVars;
    }

    @Override
    public ModelType getModelType() {
        return ModelType.OOE;
    }

    @Override
    public long getTimeToCreateVariables() {
        return timeToCreateVariables;
    }
}
