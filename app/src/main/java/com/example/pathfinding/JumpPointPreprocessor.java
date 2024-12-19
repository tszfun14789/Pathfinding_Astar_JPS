package com.example.pathfinding;

import android.os.Build;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// JumpPointPreprocessor Class
public class JumpPointPreprocessor {
    private final JpsGrid grid;
    private Map<JpsNode, List<JpsNode>> jumpPointsMap; // Precomputed jump points

    public JumpPointPreprocessor(JpsGrid grid) {
        this.grid = grid;
        this.jumpPointsMap = new HashMap<>();
    }

    // Precompute all jump points for the grid
    public void precomputeJumpPoints() {
        jumpPointsMap.clear();
        for (int i = 0; i < grid.width; i++) {
            System.out.print("Row " + i + ": ");
            for (int j = 0; j < grid.height; j++) {
                System.out.print(grid.walkable[i][j] ? "0 " : "X ");  // 'X' for obstacles, '0' for walkable
            }
            System.out.println();
        }

        // Loop through every node to find the jump points
        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                JpsNode start = new JpsNode(x, y);
                if (!grid.isWalkable(x, y)) {
                    // Skip obstacles
                    System.out.println("Node (" + x + ", " + y + ") is an obstacle.");
                    continue;
                }

                List<JpsNode> jumpPoints = new ArrayList<>();
                for (Direction dir : Direction.values()) { // Check all 8 directions
                    JpsNode jumpPoint = findJumpPoint(start, dir, grid);
                    if (jumpPoint != null) {
                        jumpPoints.add(jumpPoint);
                    }
                }
                jumpPointsMap.put(start, jumpPoints);
            }
        }

        // Print precomputed jump points
        System.out.println("Precomputed Jump Points:");
        for (Map.Entry<JpsNode, List<JpsNode>> entry : jumpPointsMap.entrySet()) {
            System.out.print("Node " + entry.getKey() + " -> Jump Points: ");
            for (JpsNode jp : entry.getValue()) {
                System.out.print("(" + jp.x + ", " + jp.y + ") ");
            }
            System.out.println();
        }
    }

    // Find the jump point in a given direction
    private JpsNode findJumpPoint(JpsNode start, Direction dir, JpsGrid grid) {
        int x = start.x;
        int y = start.y;

        while (grid.isWalkable(x, y)) {
            x += dir.dx;
            y += dir.dy;

            // Stop if out of grid boundaries
            if (!grid.isInBounds(x, y)) {
                return null;
            }

            // Check for forced neighbors
            if (grid.hasForcedNeighbor(x, y, dir)) {
                return new JpsNode(x, y);
            }

            // Stop if the node is not walkable
            if (!grid.isWalkable(x, y)) {
                return null;
            }
        }
        return null;
    }

    // Save the precomputed jump points to a file
    public void saveJumpPointsToFile(String filePath) throws IOException {
        String json = new Gson().toJson(jumpPointsMap);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.write(Paths.get(filePath), json.getBytes());
        }
    }

    // Load precomputed jump points from a file
    public void loadJumpPointsFromFile(String filePath) throws IOException {
        String content = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        }

        // Use TypeToken for correct deserialization
        jumpPointsMap = new Gson().fromJson(content, new TypeToken<Map<JpsNode, List<JpsNode>>>(){}.getType());
    }

    // Get the precomputed jump points for a node
    public List<JpsNode> getJumpPoints(JpsNode node) {
        return jumpPointsMap.getOrDefault(node, Collections.emptyList());
    }

    // Modify JPS algorithm to use precomputed jump points
    public List<JpsNode> searchWithPrecomputedJPS(JpsNode start, JpsNode goal) {
        PriorityQueue<JpsNode> openList = new PriorityQueue<>();
        Set<JpsNode> closedList = new HashSet<>();

        start.g = 0;
        start.f = heuristic(start, goal);
        openList.add(start);

        while (!openList.isEmpty()) {
            JpsNode current = openList.poll();

            if (current.equals(goal)) {
                List<JpsNode> path = reconstructPath(current);
                System.out.println("Path found: ");
                for (JpsNode step : path) {
                    System.out.println(step);
                }
                return path;
            }

            closedList.add(current);

            // Use precomputed jump points
            List<JpsNode> jumpPoints = getJumpPoints(current);

            // Add direct neighbors for traversal
            List<JpsNode> neighbors = getDirectNeighbors(current);

            for (JpsNode neighbor : neighbors) {
                if (closedList.contains(neighbor)) continue;

                double tentativeG = current.g + distance(current, neighbor);

                if (!openList.contains(neighbor) || tentativeG < neighbor.g) {
                    neighbor.g = tentativeG;
                    neighbor.f = neighbor.g + heuristic(neighbor, goal);
                    neighbor.parent = current;

                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }

            for (JpsNode jumpPoint : jumpPoints) {
                if (closedList.contains(jumpPoint)) continue;

                double tentativeG = current.g + distance(current, jumpPoint);

                if (!openList.contains(jumpPoint) || tentativeG < jumpPoint.g) {
                    jumpPoint.g = tentativeG;
                    jumpPoint.f = jumpPoint.g + heuristic(jumpPoint, goal);
                    jumpPoint.parent = current;

                    if (!openList.contains(jumpPoint)) {
                        openList.add(jumpPoint);
                    }
                }
            }
        }
        return null; // No path found
    }

    // Get direct neighbors, optimized for JPS
    private List<JpsNode> getDirectNeighbors(JpsNode node) {
        List<JpsNode> neighbors = new ArrayList<>();
        int[][] directions = {
                {-1, 0},  {1, 0},   // Up, Down
                {0, -1},  {0, 1},   // Left, Right
                {-1, -1}, {-1, 1},  // Top-left, Top-right
                {1, -1},  {1, 1}    // Bottom-left, Bottom-right
        };

        for (int[] dir : directions) {
            int newX = node.x;
            int newY = node.y;

            // Keep moving in the same direction until hitting an obstacle or boundary
            while (grid.isWalkable(newX + dir[0], newY + dir[1])) {
                newX += dir[0];
                newY += dir[1];
                neighbors.add(new JpsNode(newX, newY));
            }
        }
        return neighbors;
    }

    // Heuristic function (Manhattan distance)
    private double heuristic(JpsNode a, JpsNode b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    // Distance between two nodes
    private double distance(JpsNode a, JpsNode b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    // Reconstruct the path from goal to start
    private List<JpsNode> reconstructPath(JpsNode node) {
        List<JpsNode> jumpPoints = new ArrayList<>();
        while (node != null) {
            jumpPoints.add(node);
            node = node.parent;
        }
        Collections.reverse(jumpPoints);

        // Interpolate the full path
        return interpolatePath(jumpPoints);
    }


    private List<JpsNode> interpolatePath(List<JpsNode> jumpPoints) {
        List<JpsNode> fullPath = new ArrayList<>();

        for (int i = 0; i < jumpPoints.size() - 1; i++) {
            JpsNode start = jumpPoints.get(i);
            JpsNode end = jumpPoints.get(i + 1);

            // Add the start point
            fullPath.add(start);

            // Interpolate points between start and end
            int dx = end.x - start.x;
            int dy = end.y - start.y;

            int stepX = Integer.signum(dx); // Direction of x movement
            int stepY = Integer.signum(dy); // Direction of y movement

            int x = start.x;
            int y = start.y;

            // Add all intermediate points except the end point
            while (x != end.x || y != end.y) {
                if (x != end.x) x += stepX;
                if (y != end.y) y += stepY;

                if (x != end.x || y != end.y) {
                    fullPath.add(new JpsNode(x, y));
                }
            }
        }

        // Add the last point
        fullPath.add(jumpPoints.get(jumpPoints.size() - 1));

        return fullPath;
    }


}
