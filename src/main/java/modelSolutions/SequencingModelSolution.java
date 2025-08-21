package modelSolutions;

import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import interfaces.ModelSolutionInterface;
import enums.ModelType;

public class SequencingModelSolution implements ModelSolutionInterface {
        GRBVar[] siVars;
        GRBVar[][] yijVars;
        GRBVar[][] zijVars;
        long timeToCreateVariables;

        GRBModel model;
        int[][] earliestLatestStartTimes;

        public SequencingModelSolution(GRBVar[] siVars, GRBVar[][] yijVars, GRBVar[][] zijVars, GRBModel model, int[][] earliestLatestStartTimes, long timeToCreateVariables) {

            this.siVars = siVars;
            this.yijVars = yijVars;
            this.zijVars = zijVars;
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

        public GRBVar[] getsiVars() {
            return siVars;
        }

        public GRBVar[][] getyijVars() {
            return yijVars;
        }

        public GRBVar[][] getzijVars() {
            return zijVars;
        }

        @Override
        public ModelType getModelType() {
            return ModelType.SEQ;
        }

        @Override
        public long getTimeToCreateVariables() {
            return timeToCreateVariables;
        }
    }
