package modelSolutions;
import com.gurobi.gurobi.*;
import interfaces.ModelSolutionInterface;
import enums.ModelType;

public class DiscreteTimeModelSolution implements ModelSolutionInterface {
    GRBVar[][] startingTimeVars;
    GRBModel model;
    int[][] earliestLatestStartTimes;
    long timeToCreateVariables;

    public DiscreteTimeModelSolution(GRBVar[][] startingTimeVars, GRBModel model, int[][] earliestLatestStartTimes, long timeToCreateVariables) {
        this.startingTimeVars = startingTimeVars;
        this.model = model;
        this.earliestLatestStartTimes = earliestLatestStartTimes;
        this.timeToCreateVariables = timeToCreateVariables;
    }

    @Override
    public GRBModel getModel() {
        return model;
    }

    public int[][] getEarliestLatestStartTimes() {
        return earliestLatestStartTimes;
    }

    public GRBVar[][] getStartingTimeVars() {
        return startingTimeVars;
    }
    
    @Override
    public ModelType getModelType() {
        return ModelType.DT;
    }

    @Override
    public long getTimeToCreateVariables() {
        return timeToCreateVariables;
    }
}