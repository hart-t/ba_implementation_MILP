package models;

import io.JobDataInstance;
import models.OnOffEventBasedModel.OnOffEventBasedModelSolution;
import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import enums.ModelType;

import com.gurobi.gurobi.*;

/*
 * Tesch, A. (2020). A polyhedral study of event-based models for the resource-constrained project 
 * scheduling problem. Journal of Scheduling, 23(2), 233-251
 * 
 * https://link.springer.com/article/10.1007/s10951-020-00647-6
 * 
 */

public class IntervalEventBasedModel implements ModelInterface {
    private CompletionMethodInterface completionMethod;

    public IntervalEventBasedModel(CompletionMethodInterface completionMethod) {
        this.completionMethod = completionMethod;
    }

    @Override
    public CompletionMethodInterface getCompletionMethod() {
        return completionMethod;
    }
    
    @Override
    public GRBModel completeModel(ModelSolutionInterface initialSolution, JobDataInstance data) {

        try {
            GRBModel model = initialSolution.getModel();

            // Cast to access the Variables
            GRBVar[] startOfEventVars;
            GRBVar[][][] ziefVars;
            if (initialSolution instanceof IntervalEventBasedModel) {
                startOfEventVars = ((IntervalEventBasedModel) initialSolution)
                        .getStartOfEventEVars();
                ziefVars = ((IntervalEventBasedModel) initialSolution)
                        .getZiefVars();
            } else {
                throw new IllegalArgumentException("Expected IntervalEventBasedModel but got " +
                        initialSolution.getClass().getSimpleName());
            }

            // set the objective function to minimize the makespan
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, startOfEventVars[startOfEventVars.length - 1]);
            model.setObjective(obj, GRB.MINIMIZE);

            // (36) Add constraints for the zief variables
            for (int i = 0; i < data.numberJob; i++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int e = 0; e < startOfEventVars.length; e++) {
                    for (int f = 0; f < startOfEventVars.length; f++) {
                        expr.addTerm(1, ziefVars[i][e][f]);
                    }
                }

                model.addConstr(expr, GRB.EQUAL, 1, "zjefSum_" + i);
            }

            // (37) Event e must not exceed the resource capacities
            for (int k = 0; k < data.resourceCapacity.size(); k++) {
                for (int e = 0; e < ziefVars.length; e++) {
                    int rikSum = 0;
                    for (int i = 0; i < data.numberJob; i++) {
                        rikSum += data.jobResource[i].get(k) * ziefVars[i][e][e];
                    }
                }
            }






        return null; // Placeholder return, replace with actual GRBModel
    }

    @Override
    public boolean usesDummyJobs() {
        return true;
    }

    public class IntervallEventBasedModelSolution implements ModelSolutionInterface {
        private GRBVar makespanVar;
        private GRBVar[] startOfEventEVars;
        private GRBVar[][] jobActiveAtEventVars;
        private GRBModel model;
        private int[][] earliestLatestStartTimes;

        public IntervallEventBasedModelSolution(GRBVar makespanVar, GRBVar[] startOfEventEVars, 
                                                GRBVar[][] jobActiveAtEventVars, GRBModel model, 
                                                int[][] earliestLatestStartTimes) {
            this.makespanVar = makespanVar;
            this.startOfEventEVars = startOfEventEVars;
            this.jobActiveAtEventVars = jobActiveAtEventVars;
            this.model = model;
            this.earliestLatestStartTimes = earliestLatestStartTimes;
        }

        @Override
        public ModelType getModelType() {
            return ModelType.IEE;
        }

        @Override
        public GRBModel getModel() {
            return model;
        }
    }

    @Override
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data) {
        // Return start and finish times for jobs in this model
        return new int[0][];
    }

}
