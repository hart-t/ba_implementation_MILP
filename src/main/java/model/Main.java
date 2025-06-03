package model;

import com.gurobi.gurobi.*;

import java.io.File;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        try {
            File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
            assert files != null;

            //System.out.println("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());
            //RcpspParser.dataInstance instance = parseFile("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());

            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "rcpsp.log");
            env.start();

            GRBModel model = new GRBModel(env);

            try {
                Test.ScheduleResult result = Test.gurobiRcpspJ30("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());
                System.out.println("Start times: " + result.start);
                System.out.println("Finish times: " + result.finish);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //ZeroOneApproach ZOApproach = new ZeroOneApproach();
            //model = ZOApproach.fillModel(instance, model);

            //PritskerModel PritskerApproach = new PritskerModel();
            //model = PritskerApproach.fillModel(instance, model);


            //model.optimize();

            /*
            // Print all variables and their bounds
            System.out.println("## Variables ##");
            for (GRBVar var : model.getVars()) {
                System.out.println("Name: " + var.get(GRB.StringAttr.VarName) +
                                   ", Lower Bound: " + var.get(GRB.DoubleAttr.LB) +
                                   ", Upper Bound: " + var.get(GRB.DoubleAttr.UB));
            }

            // Print all constraints
            System.out.println("## Constraints ##");
            for (GRBConstr constr : model.getConstrs()) {
                System.out.println("Name: " + constr.get(GRB.StringAttr.ConstrName) +
                                   ", RHS: " + constr.get(GRB.DoubleAttr.RHS) +
                                   ", Sense: " + constr.get(GRB.CharAttr.Sense));
            }

            */


            //System.out.println("Total completion time: " + model.get(GRB.DoubleAttr.ObjVal));

            if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                System.out.println("Model is infeasible. Check constraints or input data.");
                model.computeIIS();    // Compute irreducible infeasible subsystem
                model.write("infeasible.ilp"); // Writes IIS to "infeasible.ilp"
            }

            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }
    }
}