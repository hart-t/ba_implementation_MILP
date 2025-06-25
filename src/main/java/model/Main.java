package model;

import java.io.File;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
        assert files != null;
        //String filename = "/home/tobsi/university/kit/benchmarkSets/" + files[0].getName();
        String filename = "/home/tobsi/university/kit/benchmarkSets/j3037_3.sm";
        //3037_4? false
        //3037_3? 63.00003099163224 starting time? 17 25?

        try {
            Result.ScheduleDoubleResult continuousFlowResult = ContinuousTimeModel.gurobiRcpspJ30(filename);
            System.out.println("Start times: " + continuousFlowResult.start());
            System.out.println("Finish times: " + continuousFlowResult.finish());
            Result.ScheduleIntegerResult timeDiscreteResult = DiscreteTimeModel.gurobiRcpspJ30(filename);
            System.out.println("Start times: " + timeDiscreteResult.start());
            System.out.println("Finish times: " + timeDiscreteResult.finish());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}