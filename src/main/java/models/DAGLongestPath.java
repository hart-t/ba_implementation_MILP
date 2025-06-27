package models;

import java.util.*;

public class DAGLongestPath {

    public static class Edge {
        public int target, weight;

        public Edge(int target, int weight) {
            this.target = target;
            this.weight = weight;
        }
    }

    private static void topologicalSort(int node, boolean[] visited, Stack<Integer> stack, List<List<Edge>> graph) {
        visited[node] = true;
        for (Edge edge : graph.get(node)) {
            if (!visited[edge.target]) {
                topologicalSort(edge.target, visited, stack, graph);
            }
        }
        stack.push(node);
    }

    public static int[] findLongestPaths(List<List<Edge>> graph, int source) {
        int n = graph.size();
        Stack<Integer> stack = new Stack<>();
        boolean[] visited = new boolean[n];
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MIN_VALUE);
        dist[source] = 0;

        // Topological sort
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                topologicalSort(i, visited, stack, graph);
            }
        }

        // Relax edges
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (dist[u] != Integer.MIN_VALUE) {
                for (Edge edge : graph.get(u)) {
                    if (dist[edge.target] < dist[u] + edge.weight) {
                        dist[edge.target] = dist[u] + edge.weight;
                    }
                }
            }
        }

        return dist;
    }

    public static int[][] generateEarliestAndLatestStartTimes(List<List<Integer>> jobPredecessors,
                                                               List<Integer> jobDuration, int horizon) {
        int[][] startTimes = new int[2][jobDuration.size()];

        int n = jobDuration.size();  // Number of nodes
        List<List<Edge>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int i = 0; i < jobPredecessors.size(); i++) {
            for (int predecessor : jobPredecessors.get(i)) {
                graph.get(predecessor - 1).add(new Edge(i, jobDuration.get(predecessor - 1)));
            }
        }

        int source = 0;
        int[] earliestStartTimes = findLongestPaths(graph, source);
        int[] latestStartTimes = new int[jobDuration.size()];

        for (int i = 0; i < jobDuration.size(); i++) {
            int duration = findLongestPaths(graph, i)[jobDuration.size() - 1];
            latestStartTimes[i] = horizon - duration;
        }


        /*for (int i = 0; i < earliestStartTimes.length; i++) {
            if (earliestStartTimes[i] == Integer.MIN_VALUE) {
                System.out.println("Node " + i + ": unreachable");
            } else {
                System.out.println("Node " + i + ": " + earliestStartTimes[i]);
                System.out.println("Node " + i + ": " + latestStartTimes[i]);
            }
        }*/

        startTimes[0] = earliestStartTimes;
        startTimes[1] = latestStartTimes;
        return startTimes;
    }
}
