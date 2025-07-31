import io.Result;
import io.FileWriter;
import java.util.List;
import java.util.ArrayList;

public class TestMerging {
    public static void main(String[] args) {
        try {
            // Create a simple test with mock results
            List<Result> results = new ArrayList<>();
            
            // Create mock results - you would need actual Result objects here
            // This is just to test if the FileWriter compiles and runs
            
            FileWriter.writeResults(results, "test_output.txt");
            System.out.println("Test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
