package model;

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
}
