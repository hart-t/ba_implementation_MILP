package model;

import com.gurobi.gurobi.*;

import java.io.File;
import java.io.FileNotFoundException;

import static model.RCPSPParser.parseFile;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        try {
            File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
            assert files != null;

            System.out.println("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());
            RCPSPParser.RCPSPInstance instance = parseFile("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());
            //RCPSPParser.RCPSPInstance instance = parseFile("/home/tobsi/university/kit/benchmarkSets/j301_1.sm");


            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "rcpsp.log");
            env.start();

            GRBModel model = new GRBModel(env);

            int numJobs = instance.numberOfJobs;
            int[] durations = instance.jobDurations;
            int[][] successors = instance.jobSuccessors;
            int[][] demands = instance.resourceRequirements;
            int[] resourceCaps = instance.resourceAvailabilities;
            int numResources = instance.numberOfResources;

            // Decision variables: start time for each job
            GRBVar[] startTimes = new GRBVar[numJobs];
            int horizon = 0;
            for (int duration : durations) horizon += duration;

            for (int i = 0; i < numJobs; i++) {
                startTimes[i] = model.addVar(0, horizon, 0, GRB.INTEGER, "start_" + i);
            }

            // Precedence constraints
            for (int i = 0; i < numJobs; i++) {
                for (int succ : successors[i]) {
                    if (succ - 1 < numJobs) {
                        GRBLinExpr prec = new GRBLinExpr();
                        prec.addTerm(1.0, startTimes[i]);
                        prec.addConstant(durations[i]);
                        model.addConstr(startTimes[succ - 1], GRB.GREATER_EQUAL, prec, "prec_" + i + "_" + succ);
                    }
                }
            }


            GRBLinExpr makespan = new GRBLinExpr();
            for (int i = 0; i < numJobs; i++) {
                makespan.addTerm(1.0, startTimes[i]);
            }





            // Define makespan as an expression: max(start_i + duration_i)
            GRBLinExpr[] finishTimes = new GRBLinExpr[numJobs];
            GRBVar[] finishVars = new GRBVar[numJobs];
            for (int i = 0; i < numJobs; i++) {
                finishVars[i] = model.addVar(0, horizon, 0, GRB.INTEGER, "finish_" + i);
                GRBLinExpr ft = new GRBLinExpr();
                ft.addTerm(1.0, startTimes[i]);
                ft.addConstant(durations[i]);
                model.addConstr(finishVars[i], GRB.EQUAL, ft, "finishConstr_" + i);
            }

            // Create a GRBVar representing the makespan
            GRBVar makespanVar = model.addVar(0, horizon, 0, GRB.INTEGER, "makespan");
            for (int i = 0; i < numJobs; i++) {
                model.addConstr(makespanVar, GRB.GREATER_EQUAL, finishVars[i], "mkspnConstr_" + i);
            }

            model.setObjective(makespan, GRB.MINIMIZE);
            model.optimize();

            for (int i = 0; i < numJobs; i++) {
                System.out.println("Start time of job " + i + ": " + startTimes[i].get(GRB.DoubleAttr.X));
            }
            System.out.println("Makespan: " + makespanVar.get(GRB.DoubleAttr.X));

            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
