package com.example.pathfinding;

import android.os.Build;

import com.google.gson.Gson;
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
        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                JpsNode start = new JpsNode(x, y);
//                if (!grid.isWalkable(x, y)) continue; // Skip obstacles
                if (!grid.isWalkable(x, y)) {
                    // Point is an obstacle, print a message and skip
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

    // Find the jump point in a given direction (simplified placeholder)
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

            // Stop for obstacles
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
        jumpPointsMap = new Gson().fromJson(content, Map.class);
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
                return reconstructPath(current);
            }

            closedList.add(current);

            // Use precomputed jump points
            List<JpsNode> jumpPoints = getJumpPoints(current);

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
        List<JpsNode> path = new ArrayList<>();
        while (node != null) {
            path.add(node);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
