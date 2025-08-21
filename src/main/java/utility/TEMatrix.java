package utility;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TEMatrix {
    public static int[][] computeTEMatrix(int jobCount, List<List<Integer>> jobSuccessors, List<Integer> jobDuration) {
        int[][] teMatrix = new int[jobCount][jobCount];

        for (int i = 0; i < jobCount; i++) {
            Set<Integer> allSuccessors = new HashSet<>();
            collectAllSuccessors(i, allSuccessors, jobSuccessors);
            for (int successor : allSuccessors) {
                teMatrix[i][successor] = 1;
            }
        }

        return teMatrix;
    }

    private static void collectAllSuccessors(int job, Set<Integer> result, List<List<Integer>> jobSuccessors) {
        for (int successor : jobSuccessors.get(job)) {
            successor -= 1;
            if (result.add(successor)) {
                collectAllSuccessors(successor, result, jobSuccessors);
            }
        }
    }
}
