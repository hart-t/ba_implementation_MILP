package logic;

import java.io.File;
import io.FileReader;
import java.util.ArrayList;
import java.util.List;

import interfaces.HeuristicInterface;
import interfaces.ModelInterface;
import io.JobDataInstance;
import models.*;
import oldtimer.Manager;
import heuristics.*;
import io.Result;
import utility.DeleteDummyJobs;

public class Main {
    public static void main(String[] args) {
        String filename = "/home/tobsi/university/kit/benchmarkSets/j303_3.sm";

        try {

        FileReader fileReader = new FileReader();
        JobDataInstance data = fileReader.dataRead(filename);
        
        ArrayList<HeuristicInterface> heuristicsList = new ArrayList<HeuristicInterface>();
        ArrayList<ModelInterface> modelList = new ArrayList<ModelInterface>();
        
        // Please add your heuristics here
        // Start with the opening heuristic, then add improvement heuristics
        //heuristicsList.add(new HeuristicSerialSGS());

        // Please add your models here
        //modelList.add(new FlowBasedContinuousTimeModel());
        //modelList.add(new DiscreteTimeModel());
        modelList.add(new OnOffEventBasedModel());

        // Create the integrated approach with the heuristics and a solver
        for (ModelInterface model : modelList) {
            IntegratedApproach integratedApproach = new IntegratedApproach(heuristicsList, new WarmstartSolver(model));

            if (!model.usesDummyJobs()) {
                JobDataInstance noDummyDataInstance = DeleteDummyJobs.deleteDummyJobs(data);
                integratedApproach.solve(noDummyDataInstance).printResult();;
            } else {
                integratedApproach.solve(data).printResult();
            }
        }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
    }
}