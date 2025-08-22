package models;

import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import solutionBuilder.BuildSequencingSolution;
import utility.TEMatrix;
import modelSolutions.SequencingModelSolution;
import interfaces.ModelSolutionInterface;
import com.gurobi.gurobi.*;
import io.JobDataInstance;

public class SequencingModel implements ModelInterface {
    CompletionMethodInterface completionMethod;

    public SequencingModel() {
        // Constructor
        this.completionMethod = new BuildSequencingSolution();
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
            GRBVar[] si;
            GRBVar[][] yij;
            GRBVar[][] zij;
            if (initialSolution instanceof SequencingModelSolution) {
                si = ((SequencingModelSolution) initialSolution).getsiVars();
                yij = ((SequencingModelSolution) initialSolution).getyijVars();
                zij = ((SequencingModelSolution) initialSolution).getzijVars();
            } else {
                throw new IllegalArgumentException("Expected DiscreteTimeModelSolution but got " + 
                                                 initialSolution.getClass().getSimpleName());
            }

            // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
            // (even if not directly), then (i,j) element TE.
            int[][] teMatrix = TEMatrix.computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

            // (1)
            // Objective function: minimize the starting time of the last job
            // This is equivalent to minimizing the makespan of the schedule
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, si[data.numberJob - 1]);
            model.setObjective(obj, GRB.MINIMIZE);

            // (2) ensure that the start of activity occurs after the completion of activity if the completion-start
            // sequencing variable is set to one. Hereby, the constant represents the scheduling horizon, i.e.,
            // an upper bound on the minimal makespan;
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-precedence
                    GRBLinExpr leftSide = new GRBLinExpr();
                    
                    leftSide.addTerm(1, si[i]);
                    leftSide.addConstant(data.jobDuration.get(i));

                    GRBLinExpr rightSide = new GRBLinExpr();
                    rightSide.addTerm(1, si[j]);
                    rightSide.addConstant(data.horizon);
                    rightSide.addTerm(-data.horizon, yij[i][j]);

                    model.addConstr(leftSide, GRB.LESS_EQUAL, rightSide, "seq_" + i + "_" + j + "_(2)");
                }
            }

            // (3) enforce the precedence relations too.
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (teMatrix[i][j] == 1) {

                        GRBLinExpr exprOne = new GRBLinExpr();
                        GRBLinExpr exprTwo = new GRBLinExpr();

                        exprOne.addTerm(1, yij[i][j]);
                        exprTwo.addTerm(1, yij[j][i]);

                        model.addConstr(exprOne, GRB.EQUAL, 1, "seq_" + i + "_" + j + "_(3)");
                        model.addConstr(exprTwo, GRB.EQUAL, 0, "seq_" + j + "_" + i + "_(3)");
                    }
                }
            }

            // (4) assure that the novel start-start sequencing variables are set according to the timing variables 
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j || teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue; // Skip self-precedence
                    GRBLinExpr leftSide = new GRBLinExpr();
                    GRBLinExpr rightSide = new GRBLinExpr();
                    leftSide.addTerm(data.horizon, zij[i][j]);
                    rightSide.addTerm(1, si[j]);
                    rightSide.addTerm(-1, si[i]);
                    rightSide.addConstant(1);
                    model.addConstr(leftSide, GRB.GREATER_EQUAL, rightSide, "seq_" + i + "_" + j + "_(4)");
                }
            }

            // (5) model the storage resource constraints. Since we are considering the special case of “standard rcpsp,” we can ignore these constraints.
            // (6) model renewable resource constraints.
            for (int i = 0; i < data.numberJob; i++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    if (data.jobResource.get(i).get(k) == 0) continue;
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addConstant(data.jobResource.get(i).get(k));
                    for (int j = 0; j < teMatrix.length; j++) {
                        if (i == j || teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue; // Skip self-precedence
                        expr.addTerm(data.jobResource.get(j).get(k), zij[j][i]);
                        expr.addTerm(-data.jobResource.get(j).get(k), yij[j][i]);
                    }
                    model.addConstr(expr, GRB.LESS_EQUAL, data.resourceCapacity.get(k), "renewable_" + i + "_" + k);
                }
            }


            return model;
        } catch (GRBException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean usesDummyJobs() {
        // This model uses dummy jobs (supersource and supersink)
        return true;
    }

    @Override
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data) {
        try {
            int[] startTimes = new int[data.numberJob];
            int[] finishTimes = new int[data.numberJob];
            
            // Initialize start times to 0
            for (int i = 0; i < data.numberJob; i++) {
                startTimes[i] = 0;
            }
            
            // Extract start times from continuous variables
            for (GRBVar var : model.getVars()) {
                String varName = var.get(GRB.StringAttr.VarName);
                if (varName.startsWith("startingTime[")) {
                    // Extract job index from variable name "startingTime[i]"
                    int jobIndex = Integer.parseInt(varName.substring("startingTime[".length(), varName.indexOf(']')));
                    double startTime = var.get(GRB.DoubleAttr.X);
                    
                    // Round to nearest integer for discrete time representation
                    if (Math.abs(startTime - Math.round(startTime)) < 0.01) {
                        startTimes[jobIndex] = (int) Math.round(startTime);
                    } else {
                        startTimes[jobIndex] = (int) Math.ceil(startTime);
                    }
                }
            }
            
            // Calculate finish times: startTime + duration
            for (int i = 0; i < data.numberJob; i++) {
                finishTimes[i] = startTimes[i] + data.jobDuration.get(i);
            }
            
            return new int[][]{startTimes, finishTimes};
            
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return null;
    }
    
}