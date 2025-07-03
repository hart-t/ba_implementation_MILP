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

    /*
     * public static int[][] computeTEMatrix2(int jobCount, List<List<Integer>> jobSuccessors, List<Integer> jobDuration) {
        int[][] teMatrix = new int[jobCount][jobCount];

        for (int i = 0; i < jobCount; i++) {
            boolean[] visited = new boolean[jobCount];
            collectCumulativeDurations(i, 0, visited, teMatrix, jobSuccessors, jobDuration);
        }

        return teMatrix;
    }

    private static void collectCumulativeDurations(
            int currentJob,
            int accumulatedDuration,
            boolean[] visited,
            int[][] teMatrix,
            List<List<Integer>> jobSuccessors,
            List<Integer> jobDuration
    ) {
        for (int successor : jobSuccessors.get(currentJob)) {
            int successorIndex = successor - 1;
            int newCumulativeDuration = accumulatedDuration + jobDuration.get(currentJob);

            // Update the matrix if no value has been set yet or if a shorter path is found
            if (teMatrix[currentJob][successorIndex] == 0 || teMatrix[currentJob][successorIndex] > newCumulativeDuration) {
                teMatrix[currentJob][successorIndex] = newCumulativeDuration;
            }

            collectCumulativeDurations(successorIndex, newCumulativeDuration, visited, teMatrix, jobSuccessors, jobDuration);
        }
    }
     */
}
