package model;

import java.io.File;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
        assert files != null;

        try {
            DiscreteTimeModel.ScheduleResult result = DiscreteTimeModel.gurobiRcpspJ30("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());
            System.out.println("Start times: " + result.start);
            System.out.println("Finish times: " + result.finish);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}