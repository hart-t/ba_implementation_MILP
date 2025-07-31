import io.FileWriter;
import io.Result;
import java.util.Arrays;
import java.util.List;

public class TestFileWriter {
    public static void main(String[] args) {
        try {
            FileWriter fileWriter = new FileWriter();
            
            // Create a simple test with empty results list
            List<Result> results = Arrays.asList();
            
            // This should create a new file with just headers
            fileWriter.writeResults("/home/tobsi/university/kit/results", "test_simple.txt", results);
            
            System.out.println("FileWriter test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error testing FileWriter: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
