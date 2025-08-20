package modelSolutions;

import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import interfaces.ModelSolutionInterface;
import enums.ModelType;

public class IntervalEventBasedModelSolution implements ModelSolutionInterface {
        private GRBVar[] startOfEventVars;
        private GRBVar[][][] ziefVars;
        private GRBModel model;
        private int[][] earliestLatestStartTimes;
        private long timeToCreateVariables;

        public IntervalEventBasedModelSolution(GRBVar[] startOfEventVars, 
                                                GRBVar[][][] ziefVars, GRBModel model, 
                                                int[][] earliestLatestStartTimes, long timeToCreateVariables) {
            this.startOfEventVars = startOfEventVars;
            this.ziefVars = ziefVars;
            this.model = model;
            this.earliestLatestStartTimes = earliestLatestStartTimes;
            this.timeToCreateVariables = timeToCreateVariables;
        }

        @Override
        public ModelType getModelType() {
            return ModelType.IEE;
        }

        @Override
        public GRBModel getModel() {
            return model;
        }

        public GRBVar[] getStartOfEventIntervalVars() {
            return startOfEventVars;
        }

        public GRBVar[][][] getjobActiveAtIntervalVars() {
            return ziefVars;
        }
        
        public int[][] getEarliestLatestStartTimes() {
            return earliestLatestStartTimes;
        }

        @Override
        public long getTimeToCreateVariables() {
            return timeToCreateVariables;
        }
    }