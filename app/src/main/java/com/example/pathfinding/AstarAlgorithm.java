package com.example.pathfinding;

import java.util.*;

public class AstarAlgorithm {

    public static List<Node> findPath(Node start, Node goal, int[][] grid) {
        int rows = grid.length;    // Rows correspond to Y-axis
        int cols = grid[0].length; // Columns correspond to X-axis

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<String, Node> allNodes = new HashMap<>(); // Key: "x,y", Value: Node

        start.gCost = 0;
        openSet.add(start);
        allNodes.put(nodeKey(start.x, start.y), start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // Check if goal is reached
            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            current.visited = true;

            for (int[] direction : new int[][]{
                    {0, -1}, {0, 1}, {-1, 0}, {1, 0},  // Cardinal directions
                    {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonal directions
            }){ // Up, Down, Left, Right
                int neighborX = current.x + direction[0]; // Columns (x-axis)
                int neighborY = current.y + direction[1]; // Rows (y-axis)

                // Adjust checks to map grid correctly
                if (!isValidPosition(neighborY, neighborX, rows, cols) || grid[neighborY][neighborX] == 1) {
                    continue; // Skip invalid or non-walkable nodes
                }

                Node neighbor = allNodes.computeIfAbsent(nodeKey(neighborX, neighborY), k -> new Node(neighborX, neighborY));
                if (neighbor.visited) {
                    continue; // Skip already visited nodes
                }

                int tentativeGCost = current.gCost + 1;
                if (tentativeGCost < neighbor.gCost) {
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = calculateHCost(neighbor, goal);
                    neighbor.parent = current;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Return empty list if no path is found
        return new ArrayList<>();
    }

    private static String nodeKey(int x, int y) {
        return x + "," + y;
    }

    private static List<Node> reconstructPath(Node current) {
        List<Node> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean isValidPosition(int y, int x, int rows, int cols) {
        return x >= 0 && y >= 0 && x < cols && y < rows; // Corrected check
    }

    private static int calculateHCost(Node from, Node to) {
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y); // Manhattan distance
    }
}
