package io;

public class TestDataReader {
    public static void main(String[] args) {
        try {
            FileReader reader = new FileReader();
            DataEvaluationInstance data = reader.getDataFromResultFile("test5.txt");
            
            System.out.println("Successfully read " + data.getDataCount() + " data rows");
            System.out.println("\nFirst 3 rows:");
            
            for (int i = 0; i < Math.min(3, data.getDataCount()); i++) {
                System.out.println("Row " + (i + 1) + ":");
                System.out.println("  Parameter: " + data.getParameter()[i]);
                System.out.println("  Instance: " + data.getInstance()[i]);
                System.out.println("  Model: " + data.getModelType()[i]);
                System.out.println("  H_M_Makespan: " + data.getHMakespan()[i]);
                System.out.println("  noH_M_Makespan: " + data.getNoHMakespan()[i]);
                System.out.println("  Time_Diff: " + data.getTimeDiff()[i]);
                System.out.println("  Time_Limit_Reached: " + data.getTimeLimitReached()[i]);
                System.out.println("  Heuristics: " + data.getHeuristics()[i]);
                System.out.println();
            }
            
            // Show some statistics
            long totalRows = data.getDataCount();
            long onoffRows = 0;
            long discRows = 0;
            long flowRows = 0;
            
            for (int i = 0; i < data.getDataCount(); i++) {
                String model = data.getModelType()[i];
                switch (model) {
                    case "ONOFF": onoffRows++; break;
                    case "DISC": discRows++; break;
                    case "FLOW": flowRows++; break;
                }
            }
            
            System.out.println("Statistics:");
            System.out.println("  Total rows: " + totalRows);
            System.out.println("  DISC model rows: " + discRows);
            System.out.println("  FLOW model rows: " + flowRows);
            System.out.println("  ONOFF model rows: " + onoffRows);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
