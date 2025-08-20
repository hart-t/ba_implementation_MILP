package models;

import io.JobDataInstance;
import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import enums.ModelType;
import solutionBuilder.BuildIntervalEventSolution;
import utility.DeleteDummyJobs;

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
            GRBVar[] startOfEventIntervalVars;
            GRBVar[][][] jobActiveAtIntervalVars;
            if (initialSolution instanceof IntervalEventBasedModelSolution) {
                startOfEventIntervalVars = ((IntervalEventBasedModelSolution) initialSolution)
                        .getStartOfEventIntervalVars();
                jobActiveAtIntervalVars = ((IntervalEventBasedModelSolution) initialSolution)
                        .getjobActiveAtIntervalVars();
            } else {
                throw new IllegalArgumentException("Expected IntervalEventBasedModelSolution but got " +
                        initialSolution.getClass().getSimpleName());
            }

            // delete dummy jobs from data and earliestLatestStartTimes
            data = DeleteDummyJobs.deleteDummyJobs(data);

            // set the objective function to minimize the makespan
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, startOfEventIntervalVars[startOfEventIntervalVars.length - 1]);
            model.setObjective(obj, GRB.MINIMIZE);

            // int counter = 0;
            // (36) Add constraints for the jobActiveAtIntervalVars
            for (int i = 0; i < data.numberJob; i++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int e = 0; e < startOfEventIntervalVars.length - 1; e++) {
                    for (int f = e + 1; f < startOfEventIntervalVars.length; f++) {
                        expr.addTerm(1, jobActiveAtIntervalVars[i][e][f]);
                    }
                }
                model.addConstr(expr, GRB.EQUAL, 1, "jobActiveAtIntervalVarsSum_" + i);
                // counter++;
            }

            // System.out.println(counter);
            // counter = 0;

            // (37) Event e must not exceed the resource capacities
            for (int k = 0; k < data.resourceCapacity.size(); k++) {
                for (int e = 0; e < startOfEventIntervalVars.length - 1; e++) {
                    GRBLinExpr expr = new GRBLinExpr();

                    for (int i = 0; i < data.numberJob; i++) {
                        for (int ee = 0; ee <= e; ee++) {
                            for (int ff = e + 1; ff < startOfEventIntervalVars.length; ff++) {
                                expr.addTerm(data.jobResource.get(i).get(k), jobActiveAtIntervalVars[i][ee][ff]);
                            }
                        }
                    }
                    model.addConstr(expr, GRB.LESS_EQUAL, data.resourceCapacity.get(k), "resourceCapacity_" + k + "_event_" + e);
                    // counter++;
                }
            }

            // System.out.println(counter);
            // counter = 0;

            // (38)
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 0; e < startOfEventIntervalVars.length - 1; e++) {
                    for (int f = e + 1; f < startOfEventIntervalVars.length; f++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr.addTerm(1, startOfEventIntervalVars[e]);

                        for (int ee = e; ee < f; ee++) {
                            for (int ff = ee + 1; ff <= f; ff++) {
                                expr.addTerm(data.jobDuration.get(i), jobActiveAtIntervalVars[i][ee][ff]);
                            }
                        }
                        expr2.addTerm(1, startOfEventIntervalVars[f]);

                        model.addConstr(expr, GRB.LESS_EQUAL, expr2, "job_" + i + "_event_" + e + "_" + f);
                        // counter++;
                    }
                }
            }

            // System.out.println(counter);
            // counter = 0;

            // (39)
            for (int j = 0; j < data.numberJob; j++) {
                for (int i : data.jobPredecessors.get(j)) {
                    if (j == 5) System.out.println("Processing predecessor constraint for job " + j + " and predecessor " + i);
                    for (int e = 0; e < startOfEventIntervalVars.length - 1; e++) {
                        GRBLinExpr expr = new GRBLinExpr();

                        for (int ee = 0; ee < startOfEventIntervalVars.length - 1; ee++) {
                            for (int ff = Math.max(e + 1, ee + 1); ff < startOfEventIntervalVars.length; ff++) {
                                expr.addTerm(1, jobActiveAtIntervalVars[i][ee][ff]);
                            }
                        }

                        for (int ee = 0; ee <= e; ee++) {
                            for (int ff = e + 1; ff < startOfEventIntervalVars.length; ff++) {
                                expr.addTerm(1, jobActiveAtIntervalVars[j][ee][ff]);
                            }
                        }
                        model.addConstr(expr, GRB.LESS_EQUAL, 1, j + "predecessor_" + i + "_" + e);
                        // counter++;
                    }
                }
            }

            // System.out.println(counter);
            // counter = 0;

            return model;
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

    @Override
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data) {
        try {
            // This model uses dummy jobs, so we need to work with the data without dummy jobs
            JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);
            
            int[] startTimes = new int[noDummyData.numberJob];
            int[] finishTimes = new int[noDummyData.numberJob];
            
            // Initialize start times to -1 (not found)
            for (int i = 0; i < noDummyData.numberJob; i++) {
                startTimes[i] = -1;
            }
            
            // Extract event interval start times
            double[] eventStartTimes = new double[noDummyData.numberJob + 1];
            for (int e = 0; e <= noDummyData.numberJob; e++) {
                GRBVar eventVar = model.getVarByName("startOfEvent[" + e + "]");
                if (eventVar != null) {
                    eventStartTimes[e] = eventVar.get(GRB.DoubleAttr.X);
                    System.out.println("Event " + e + " starts at time: " + eventStartTimes[e]);
                }
            }
            
            // Find start times by checking which interval each job is active in
            for (int i = 0; i < noDummyData.numberJob; i++) {
                for (int e = 0; e < noDummyData.numberJob; e++) {
                    for (int f = e + 1; f <= noDummyData.numberJob; f++) {
                        try {
                            GRBVar activeVar = model.getVarByName("jobActiveAtIntervalVars[" + i + "][" + e + "][" + f + "]");
                            
                            
                            if (activeVar != null && activeVar.get(GRB.DoubleAttr.X) > 0.5) {
                                if (i == 5 || i == 1) System.out.println("Job " + i + " is active in interval [" + e + ", " + f + ")");
                                System.out.println(eventStartTimes[e]);
                                System.out.println(eventStartTimes[f]);
                                // Job i is active in interval [e, f), so it starts at event e
                                startTimes[i] = (int) Math.round(eventStartTimes[e]);
                                break;
                            }
                        } catch (GRBException ex) {
                            // Variable doesn't exist, continue
                        }
                    }
                    
                    // Break outer loop if start time found
                    if (startTimes[i] >= 0) {
                        break;
                    }
                }
            }
            
            // Calculate finish times: startTime + duration
            for (int i = 0; i < noDummyData.numberJob; i++) {
                if (startTimes[i] >= 0) {
                    finishTimes[i] = startTimes[i] + noDummyData.jobDuration.get(i);
                } else {
                    finishTimes[i] = -1; // Job not scheduled
                }
            }
            
            return new int[][]{startTimes, finishTimes};
            
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return null;
    }

}
