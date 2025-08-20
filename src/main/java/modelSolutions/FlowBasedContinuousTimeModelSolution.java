package modelSolutions;

import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import interfaces.ModelSolutionInterface;
import enums.ModelType;

public class FlowBasedContinuousTimeModelSolution implements ModelSolutionInterface {
        GRBVar[] startingTimeVars;
        GRBVar[][] precedenceVars;
        GRBVar[][][] continuousFlowVars;
        long timeToCreateVariables;

        GRBModel model;
        int[][] earliestLatestStartTimes;

        public FlowBasedContinuousTimeModelSolution(GRBVar[] startingTimeVars, GRBVar[][] precedenceVars,
                        GRBVar[][][] continuousFlowVars, GRBModel model, int[][] earliestLatestStartTimes, long timeToCreateVariables) {

            this.startingTimeVars = startingTimeVars;
            this.precedenceVars = precedenceVars;
            this.continuousFlowVars = continuousFlowVars;
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

        public GRBVar[] getStartingTimeVars() {
            return startingTimeVars;
        }

        public GRBVar[][] getPrecedenceVars() {
            return precedenceVars;
        }

        public GRBVar[][][] getContinuousFlowVars() {
            return continuousFlowVars;
        }

        @Override
        public ModelType getModelType() {
            return ModelType.FLOW;
        }

        @Override
        public long getTimeToCreateVariables() {
            return timeToCreateVariables;
        }
    }
