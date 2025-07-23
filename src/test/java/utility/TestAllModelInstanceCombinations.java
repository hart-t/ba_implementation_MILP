package utility;

import io.FileReader;
import io.JobDataInstance;
import logic.IntegratedApproach;
import logic.WarmstartSolver;
import models.OnOffEventBasedModel;
import models.DiscreteTimeModel;
import models.FlowBasedContinuousTimeModel;

import java.util.ArrayList;
import java.io.File;

import interfaces.HeuristicInterface;
import interfaces.ModelInterface;

public class TestAllModelInstanceCombinations {
    
    public static void main(String[] args) {

        File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
        assert files != null;
        String filename = "";


        for (File file : files) {
            filename = "/home/tobsi/university/kit/benchmarkSets/" + file.getName();
            

            try {

                FileReader fileReader = new FileReader();
                JobDataInstance data = fileReader.dataRead(filename);
                
                ArrayList<HeuristicInterface> heuristicsList = new ArrayList<HeuristicInterface>();
                ArrayList<ModelInterface> modelList = new ArrayList<ModelInterface>();
                
                // Please add your heuristics here
                // Start with the opening heuristic, then add improvement heuristics
                // insert the priority rule for the SGS heuristic, default is shortest processing time first
                // heuristicsList.add(new HeuristicSerialSGS());

                // Please add your models here
                modelList.add(new FlowBasedContinuousTimeModel());
                modelList.add(new DiscreteTimeModel());
                modelList.add(new OnOffEventBasedModel());

                // Create the integrated approach with the heuristics and a solver
                for (ModelInterface model : modelList) {
                    IntegratedApproach integratedApproach = new IntegratedApproach(heuristicsList, new WarmstartSolver(model));

                    integratedApproach.solve(data).printResult();
                    
            }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return;
            }
        }
    }
}
