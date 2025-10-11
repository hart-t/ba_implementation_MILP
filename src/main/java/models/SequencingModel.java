package models;

import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import solutionBuilder.BuildSequencingSolution;
import utility.TEMatrix;
import modelSolutions.SequencingModelSolution;
import interfaces.ModelSolutionInterface;
import com.gurobi.gurobi.*;
import io.JobDataInstance;
import java.util.ArrayList;
import java.util.List;

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

            // for further enhancements, we compute two sets of Variables: F2 and F3

            // Compute F2: pairs (i,j) where i < j, no precedence relation exists,
            // and combined resource requirement exceeds capacity of at least one resource
            List<int[]> F2 = new ArrayList<>();
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = i + 1; j < data.numberJob; j++) {
                    // Check if (i,j) and (j,i) are not in TE (no precedence relation)
                    if (teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue;
                    
                    // Check if there exists a resource k where r_ik + r_jk > R_k
                    boolean exceedsCapacity = false;
                    for (int k = 0; k < data.resourceCapacity.size(); k++) {
                        int resourceSum = data.jobResource.get(i).get(k) + data.jobResource.get(j).get(k);
                        if (resourceSum > data.resourceCapacity.get(k)) {
                            exceedsCapacity = true;
                            break;
                        }
                    }
                    
                    if (exceedsCapacity) {
                        F2.add(new int[]{i, j});
                    }
                }
            }

            // Compute F3: triples (i,j,h) where i < j < h, no precedence relations exist between any pair,
            // none of the pairs (i,j), (i,h), (j,h) are in F2,
            // and combined resource requirement exceeds capacity of at least one resource
            List<int[]> F3 = new ArrayList<>();
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = i + 1; j < data.numberJob; j++) {
                    for (int h = j + 1; h < data.numberJob; h++) {
                        // Check if no precedence relations exist
                        if (teMatrix[i][j] == 1 || teMatrix[j][i] == 1 ||
                            teMatrix[i][h] == 1 || teMatrix[h][i] == 1 ||
                            teMatrix[j][h] == 1 || teMatrix[h][j] == 1) continue;
                        
                        // Check if (i,j), (i,h), (j,h) are not in F2
                        boolean inF2 = false;
                        for (int[] pair : F2) {
                            if ((pair[0] == i && pair[1] == j) ||
                                (pair[0] == i && pair[1] == h) ||
                                (pair[0] == j && pair[1] == h)) {
                                inF2 = true;
                                break;
                            }
                        }
                        if (inF2) continue;
                        
                        // Check if there exists a resource k where r_ik + r_jk + r_hk > R_k
                        boolean exceedsCapacity = false;
                        for (int k = 0; k < data.resourceCapacity.size(); k++) {
                            int resourceSum = data.jobResource.get(i).get(k) + 
                                            data.jobResource.get(j).get(k) + 
                                            data.jobResource.get(h).get(k);
                            if (resourceSum > data.resourceCapacity.get(k)) {
                                exceedsCapacity = true;
                                break;
                            }
                        }
                        
                        if (exceedsCapacity) {
                            F3.add(new int[]{i, j, h});
                        }
                    }
                }
            }

            // (7) For each pair in F2, at least one sequencing variable must be 1
            for (int[] pair : F2) {
                int i = pair[0];
                int j = pair[1];
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1, yij[i][j]);
                expr.addTerm(1, yij[j][i]);
                model.addConstr(expr, GRB.GREATER_EQUAL, 1, "F2_" + i + "_" + j + "_(7)");
            }

            // (8) For each triple in F3, at least one sequencing variable must be 1
            // This ensures that for three activities that cannot run simultaneously,
            // at least one pair has a completion-start ordering
            for (int[] triple : F3) {
                int i = triple[0];
                int j = triple[1];
                int h = triple[2];
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1, yij[i][j]);
                expr.addTerm(1, yij[j][i]);
                expr.addTerm(1, yij[i][h]);
                expr.addTerm(1, yij[h][i]);
                expr.addTerm(1, yij[j][h]);
                expr.addTerm(1, yij[h][j]);
                model.addConstr(expr, GRB.GREATER_EQUAL, 1, "F3_" + i + "_" + j + "_" + h + "_(8)");
            }

            // (9) Transitivity constraints: activity i cannot be completed before the start of activity j
            // if activity j is completed before the start of activity i
            // This applies to all pairs (i,j) where i < j and no precedence relation exists
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = i + 1; j < data.numberJob; j++) {
                    // Check if (i,j) and (j,i) are not in TE (no precedence relation)
                    if (teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue;
                    
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1, yij[i][j]);
                    expr.addTerm(1, yij[j][i]);
                    model.addConstr(expr, GRB.LESS_EQUAL, 1, "transitivity_" + i + "_" + j + "_(9)");
                }
            }

            // (10) For any pair of activities, at least one activity has to start before the other
            // or both activities must start at the same time
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = i + 1; j < data.numberJob; j++) {
                    // Check if (i,j) and (j,i) are not in TE (no precedence relation)
                    if (teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue;
                    
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1, zij[i][j]);
                    expr.addTerm(1, zij[j][i]);
                    model.addConstr(expr, GRB.GREATER_EQUAL, 1, "start_ordering_" + i + "_" + j + "_(10)");
                }
            }

            // (11) If activity i is completed before the start of activity j,
            // then activity i also has to start before j
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j || teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue;
                    
                    GRBLinExpr leftSide = new GRBLinExpr();
                    leftSide.addTerm(1, zij[i][j]);
                    
                    GRBLinExpr rightSide = new GRBLinExpr();
                    rightSide.addTerm(1, yij[i][j]);
                    
                    model.addConstr(leftSide, GRB.GREATER_EQUAL, rightSide, "start_before_complete_" + i + "_" + j + "_(11)");
                }
            }

            // (12) If both z_ij and z_ji are equal to one, then the two activities must start at the same time
            // This is modeled as: 2 - z_ij - z_ji >= (1/T)(S_i - S_j)
            // Since we need this for both directions, we add constraints for both (S_i - S_j) and (S_j - S_i)
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = i + 1; j < data.numberJob; j++) {
                    if (teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue;
                    
                    // 2 - z_ij - z_ji >= (1/T)(S_i - S_j)
                    GRBLinExpr leftSide1 = new GRBLinExpr();
                    leftSide1.addConstant(2);
                    leftSide1.addTerm(-1, zij[i][j]);
                    leftSide1.addTerm(-1, zij[j][i]);
                    
                    GRBLinExpr rightSide1 = new GRBLinExpr();
                    rightSide1.addTerm(1.0 / data.horizon, si[i]);
                    rightSide1.addTerm(-1.0 / data.horizon, si[j]);
                    
                    model.addConstr(leftSide1, GRB.GREATER_EQUAL, rightSide1, "same_start_" + i + "_" + j + "_(12a)");
                    
                    // 2 - z_ij - z_ji >= (1/T)(S_j - S_i)
                    GRBLinExpr leftSide2 = new GRBLinExpr();
                    leftSide2.addConstant(2);
                    leftSide2.addTerm(-1, zij[i][j]);
                    leftSide2.addTerm(-1, zij[j][i]);
                    
                    GRBLinExpr rightSide2 = new GRBLinExpr();
                    rightSide2.addTerm(1.0 / data.horizon, si[j]);
                    rightSide2.addTerm(-1.0 / data.horizon, si[i]);
                    
                    model.addConstr(leftSide2, GRB.GREATER_EQUAL, rightSide2, "same_start_" + j + "_" + i + "_(12b)");
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