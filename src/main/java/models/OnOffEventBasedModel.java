package models;

import com.gurobi.gurobi.*;

import io.JobDataInstance;
import io.Result;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import interfaces.CompletionMethodInterface;
import solutionBuilder.BuildOnOffEventSolution;

/**
 * On off Event-Based Model 2
 *
 * https://www.sciencedirect.com/science/article/pii/S0305054809003360
 *      on off event-based model
 *
 *      public final int numberJob;
 *      public final int horizon;
 *      public final List<Integer> jobNumSuccessors;
 *      public final List<List<Integer>> jobSuccessors;
 *      public final List<List<Integer>> jobPredecessors;
 *      public final List<Integer> jobDuration;
 *      public final List<List<Integer>> jobResource;
 *      public final List<Integer> resourceCapacity;
 * 
 *      In this model, the number of events is exactly equal to the number of activities, n.
 *      event-based formulations do not involve the use of dummy activities.
 * */

public class OnOffEventBasedModel implements ModelInterface {
    private final CompletionMethodInterface completionMethod;

    public OnOffEventBasedModel() {
        // Constructor
        this.completionMethod = new BuildOnOffEventSolution();
    }

    @Override
    public CompletionMethodInterface getCompletionMethod() {
        return completionMethod;
    }
    
    public GRBModel completeModel(ModelSolutionInterface initialSolution, JobDataInstance data) {
        try {
            GRBModel model = initialSolution.getModel();

            // Cast to access the Variables
            GRBVar[] startOfEventEVars;
            GRBVar[][] jobActiveAtEventVars;
            int[] earliestStartTime;
            int[] latestStartTime;
            if (initialSolution instanceof OnOffEventBasedModelSolution) {
                startOfEventEVars = ((OnOffEventBasedModelSolution) initialSolution)
                        .getStartOfEventEVars();
                jobActiveAtEventVars = ((OnOffEventBasedModelSolution) initialSolution)
                        .getJobActiveAtEventVars();
                earliestStartTime = ((OnOffEventBasedModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[0];
                latestStartTime = ((OnOffEventBasedModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[1];
            } else {
                throw new IllegalArgumentException("Expected DiscreteTimeModelSolution but got " + 
                                                 initialSolution.getClass().getSimpleName());
            }

            // (41) We also use one single extra continuous variable Cmax to represent the makespan of the 
            // schedule. Set the objective to minimize Cmax
            GRBVar makespanVar = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "makespan");
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, makespanVar);
            model.setObjective(obj, GRB.MINIMIZE);

            // (42) Constraints (42) ensure that each activity is processed at least once during the project.
            for (int i = 0; i < data.numberJob; i++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                for (int e = 0; e < startOfEventEVars.length; e++) {
                    expr1.addTerm(1, jobActiveAtEventVars[i][e]);
                }
                model.addConstr(expr1, GRB.GREATER_EQUAL, 1, expr1 + "greater equal 1 (42)");
            }

            // (43) Constraints (43) link the makespan to the event dates: Cmax >= te + (zie - zi,e-1)pi
            // for all e in E, all i in A
            for (int i = 0; i < data.numberJob; i++) {
                // start at e = 1 to ensure e-1 >= 0
                for (int e = 1; e < startOfEventEVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    
                    expr1.addTerm(1, makespanVar);
                    
                    expr2.addTerm(1, startOfEventEVars[e]);
                    expr2.addTerm(data.jobDuration.get(i), jobActiveAtEventVars[i][e]);
                    expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEventVars[i][e - 1]);
                    
                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2 , "makespan_constraint_" + i + "_" + e + "(43)");
                }
            }

            // (44), (45) Constraints (44), (45) ensure event sequencing.
            // TODO im paper steht "für alle e, ausgenommen n-1", es müsste "ausgenommen n" sein da man 
            // sonst auf event n+1 zugreifen würde
            GRBLinExpr expr01 = new GRBLinExpr();
            expr01.addTerm(1, startOfEventEVars[0]);
            model.addConstr(expr01, GRB.EQUAL, 0, "event 0 starts at time dated 0 (44)");

            // skip event n
            for (int e = 0; e < startOfEventEVars.length - 1; e++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, startOfEventEVars[e + 1]);
                expr2.addTerm(1, startOfEventEVars[e]);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + " starts after " + expr2 + "(45)");
            }

            // (46) Constraints (46) link the binary optimization variables zie to the continuous optimization
            //  variables te, and ensure that, if activity i starts immediately after event e and ends at event 
            // f, then the date of event f is at least equal to the date of event e plus the processing time of
            // activity i (tf >= te + pi)
            // start at 1 or 0 TODO
            for (int e = 1; e < startOfEventEVars.length; e++) {
                for (int f = 1; f < startOfEventEVars.length; f++) {
                    if (f <= e) {
                        continue;
                    }
                    for (int i = 0; i < data.numberJob; i++) {
                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, startOfEventEVars[f]);

                        expr2.addTerm(1, startOfEventEVars[e]);
                        expr2.addTerm(data.jobDuration.get(i), jobActiveAtEventVars[i][e]);
                        expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEventVars[i][e - 1]);
                        expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEventVars[i][f]);
                        expr2.addTerm(data.jobDuration.get(i), jobActiveAtEventVars[i][f - 1]);
                        expr2.addConstant(- data.jobDuration.get(i));

                        model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + " greater equal " + expr2 + "(46)");
                    }
                }
            }

            // (47) if activity i starts at event e, then i cannot be processed before e.
            // TODO missing "für alle i element A"
            // Constraints (47), (48), called contiguity constraints, ensure non-preemption 
            // (the events after which a given activity is being processed are adjacent).
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 1; e < startOfEventEVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int ee = 0; ee < e - 1; ee++) {
                        expr1.addTerm(1, jobActiveAtEventVars[i][ee]);
                    }

                    expr2.addConstant(e);
                    expr2.addTerm(- e, jobActiveAtEventVars[i][e]);
                    expr2.addTerm(e, jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (47)");
                }
            }

            // (48) 
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 1; e < startOfEventEVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    // Fix: ensure ee doesn't go out of bounds
                    for (int ee = e; ee < Math.min(data.numberJob, startOfEventEVars.length); ee++) {
                        expr1.addTerm(1, jobActiveAtEventVars[i][ee]);
                    }

                    expr2.addConstant((data.numberJob - e));
                    expr2.addTerm((data.numberJob - e), jobActiveAtEventVars[i][e]);
                    expr2.addTerm(- (data.numberJob - e), jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (48)");
                }
            }

            // Constraints (49) describe each precedence constraint (i,j) element E(precedence graph).
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.jobPredecessors.get(i).size(); j++) {
                    int predecessor = data.jobPredecessors.get(i).get(j);
                    // predecessor is 1-indexed, so we need to subtract 1 TODO maybe not necessary
                    predecessor -= 1;

                    // if activity i starts at event e, then it cannot be processed before event f
                    for (int e = 0; e < startOfEventEVars.length; e++) {
                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, jobActiveAtEventVars[i][e]);
                        for (int ee = 0; ee < e; ee++) {
                            expr1.addTerm(1, jobActiveAtEventVars[predecessor][ee]);
                        }

                        expr2.addConstant((1 + e));
                        expr2.addTerm(- e, jobActiveAtEventVars[i][e]);

                        model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + i + " predecessor: " + predecessor + " (49) ");
                    }
                }
            }

            // (50) the resource constraints limiting the total demand of activities in process at each event.
            for (int e = 0; e < startOfEventEVars.length; e++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    // TODO wieder nur bis n - 1?
                    for (int i = 0; i < data.numberJob; i++) {
                        expr1.addTerm(data.jobResource.get(i).get(k), jobActiveAtEventVars[i][e]);
                    }
                    model.addConstr(expr1, GRB.LESS_EQUAL, data.resourceCapacity.get(k),
                        "resource_" + k + "_event_" + e + " (50)");
                }
            }

            // (51) Constraints (51) and (52) set the start time of any activity i between its earliest start 
            // time ESi and its latest start time LSi.
            for (int e = 1; e < startOfEventEVars.length; e++) {
                for (int i = 0; i < data.numberJob; i++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    GRBLinExpr expr3 = new GRBLinExpr();
    
                    expr1.addTerm(earliestStartTime[i], jobActiveAtEventVars[i][e]);

                    expr2.addTerm(1, startOfEventEVars[e]);

                    expr3.addTerm(latestStartTime[i], jobActiveAtEventVars[i][e]);
                    expr3.addTerm(- latestStartTime[i], jobActiveAtEventVars[i][e - 1]);
                    expr3.addConstant(latestStartTime[latestStartTime.length - 1]);
                    expr3.addTerm(- latestStartTime[latestStartTime.length - 1], jobActiveAtEventVars[i][e]);
                    expr3.addTerm(latestStartTime[latestStartTime.length - 1], jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (51)");
                    model.addConstr(expr2, GRB.LESS_EQUAL, expr3, expr2 + " less equal " + expr3 + " (51)");   
                }
            }

            // (52) TODO für alle e und für alle i?
            // ESi <= makespanVar <= LSi
            GRBLinExpr expr02 = new GRBLinExpr();
            expr02.addTerm(1, makespanVar);
            model.addConstr(earliestStartTime[earliestStartTime.length - 1], GRB.LESS_EQUAL, expr02,
                "ESn+1 less equal makespanVar (52)");
            model.addConstr(expr02, GRB.LESS_EQUAL, latestStartTime[latestStartTime.length - 1],
                "makespanVar less equal LSn+1 (52)");

            // (53) te >= 0 for all e in E, unnessesary as we set the lower bound to 0 when creating the 
            // variable
            // (54) zie element {0,1} for all i in A, e in E, unnessesary as we set the type to binary when
            // creating the variable

            return model;

        } catch (Exception e) {
            System.err.println("Error while solving the model: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean usesDummyJobs() {
        // This model does not use dummy jobs
        return false;
    }

    public class OnOffEventBasedModelSolution implements ModelSolutionInterface {
        GRBVar[] startOfEventEVars;
        GRBVar[][] jobActiveAtEventVars;

        GRBModel model;
        int[][] earliestLatestStartTimes;

        public OnOffEventBasedModelSolution(GRBVar[] startOfEventE, GRBVar[][] jobActiveAtEvent, 
                                            GRBModel model, int[][] earliestLatestStartTimes) {
            this.startOfEventEVars = startOfEventE;
            this.jobActiveAtEventVars = jobActiveAtEvent;
            this.earliestLatestStartTimes = earliestLatestStartTimes;
            this.model = model;
        }

        @Override
        public GRBModel getModel() {
            return model;
        }

        public int[][] getEarliestLatestStartTimes() {
            return earliestLatestStartTimes;
        }

        public GRBVar[] getStartOfEventEVars() {
            return startOfEventEVars;
        }

        public GRBVar[][] getJobActiveAtEventVars() {
            return jobActiveAtEventVars;
        }
    }
}
