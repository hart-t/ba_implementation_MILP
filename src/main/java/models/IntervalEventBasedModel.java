package models;

import io.JobDataInstance;
import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import enums.ModelType;
import solutionBuilder.BuildIntervalEventSolution;

import com.gurobi.gurobi.*;

/*
 * Tesch, A. (2020). A polyhedral study of event-based models for the resource-constrained project 
 * scheduling problem. Journal of Scheduling, 23(2), 233-251
 * 
 * https://link.springer.com/article/10.1007/s10951-020-00647-6
 * 
 */

public class IntervalEventBasedModel implements ModelInterface {
    private final CompletionMethodInterface completionMethod;

    public IntervalEventBasedModel() {
        this.completionMethod = new BuildIntervalEventSolution();
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
            if (initialSolution instanceof IntervalEventBasedModelSolution) {
                startOfEventVars = ((IntervalEventBasedModelSolution) initialSolution)
                        .getStartOfEventVars();
                ziefVars = ((IntervalEventBasedModelSolution) initialSolution)
                        .getZiefVars();
            } else {
                throw new IllegalArgumentException("Expected IntervalEventBasedModelSolution but got " +
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
                for (int e = 0; e < data.numberJob; e++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int i = 0; i < data.numberJob; i++) {
                        expr.addTerm(data.jobResource.get(i).get(k), ziefVars[i][e][e]);
                        for (int ee = 0; ee <= e; ee++) {
                            for (int ff = e + 1; ff < data.numberJob; ff++) {
                                expr.addTerm(data.jobResource.get(i).get(k), ziefVars[i][ee][ff]);
                            }
                        }
                    }
                    model.addConstr(expr, GRB.LESS_EQUAL, data.resourceCapacity.get(k), "resourceCap_" + k + "_event_" + e);
                }
            }

            // (38) Moreover, inequalities (38) say that if job j is assigned an event interval [e′, f ′ ] ⊆ [e, f ], then t e + p j ≤ t f musthold.
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 0; e < startOfEventVars.length; e++) {
                    for (int f = e + 1; f < startOfEventVars.length; f++) {
                        GRBLinExpr leftSide = new GRBLinExpr();
                        leftSide.addTerm(1, startOfEventVars[e]);
                        leftSide.addTerm(data.jobDuration.get(i), ziefVars[i][e][f]);

                        GRBLinExpr rightSide = new GRBLinExpr();
                        rightSide.addTerm(1, startOfEventVars[f]);

                        model.addConstr(leftSide, GRB.LESS_EQUAL, rightSide, "eventInterval_" + i + "_" + e + "_" + f);
                    }
                }
            }




        return model; // Placeholder return, replace with actual GRBModel
        } catch (GRBException e) {
            System.err.println("Error while completing the model: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean usesDummyJobs() {
        return true;
    }

    public class IntervalEventBasedModelSolution implements ModelSolutionInterface {
        private GRBVar[] startOfEventVars;
        private GRBVar[][][] ziefVars;
        private GRBModel model;
        private int[][] earliestLatestStartTimes;

        public IntervalEventBasedModelSolution(GRBVar[] startOfEventVars, 
                                                GRBVar[][][] ziefVars, GRBModel model, 
                                                int[][] earliestLatestStartTimes) {
            this.startOfEventVars = startOfEventVars;
            this.ziefVars = ziefVars;
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

        public GRBVar[] getStartOfEventVars() {
            return startOfEventVars;
        }

        public GRBVar[][][] getZiefVars() {
            return ziefVars;
        }
    }

    @Override
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data) {
        // Return start and finish times for jobs in this model
        return new int[0][];
    }

}
