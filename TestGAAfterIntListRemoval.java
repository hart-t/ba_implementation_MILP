import heuristics.geneticAlgorithm.*;
import io.JobDataInstance;
import java.util.*;

public class TestGAAfterIntListRemoval {
    public static void main(String[] args) {
        // Create a simple test project manually
        int J = 3; // 3 non-dummy activities
        int K = 1; // 1 resource
        int[] p = {0, 2, 3, 1, 0}; // durations: dummy0, job1, job2, job3, dummyEnd
        int[][] r = {{0}, {1}, {1}, {1}, {0}}; // resource demands
        int[] R = {2}; // resource capacity
        int horizon = 10;
        
        // Create predecessor/successor lists
        @SuppressWarnings("unchecked")
        List<Integer>[] P = (List<Integer>[]) new List[5];
        @SuppressWarnings("unchecked")
        List<Integer>[] S = (List<Integer>[]) new List[5];
        
        for (int i = 0; i < 5; i++) {
            P[i] = new ArrayList<>();
            S[i] = new ArrayList<>();
        }
        
        // Simple precedence: 0 -> 1 -> 2 -> 3 -> 4
        S[0].add(1);
        P[1].add(0);
        
        S[1].add(2);
        P[2].add(1);
        
        S[2].add(3);
        P[3].add(2);
        
        S[3].add(4);
        P[4].add(3);
        
        // Create project
        Project project = new Project(J, K, p, r, R, P, S, horizon);
        
        // Test GA
        System.out.println("Testing GA after IntList removal...");
        HartmannGA ga = new HartmannGA(project, 12345L);
        Individual result = ga.run();
        
        System.out.println("SUCCESS! GA worked without IntList");
        System.out.println("Best makespan: " + result.getFitness());
        System.out.println("Activity list: " + result.toString());
        
        System.out.println("IntList successfully replaced with List<Integer>!");
    }
}
