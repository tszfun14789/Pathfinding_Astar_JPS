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

        // Only process nodes that could potentially be jump points
        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                if (!grid.isWalkable(x, y)) {
                    continue;
                }

                // Only process nodes near obstacles or at grid boundaries
                if (!isNearObstacleOrBoundary(x, y)) {
                    continue;
                }

                JpsNode start = new JpsNode(x, y);
                List<JpsNode> jumpPoints = new ArrayList<>();

                // For straight directions, only process if there's an obstacle adjacent
                for (Direction dir : Direction.values()) {
                    if (!dir.isDiagonal() && !hasAdjacentObstacle(x, y, dir)) {
                        continue;
                    }
                    JpsNode jumpPoint = findJumpPoint(start, dir, grid);
                    if (jumpPoint != null) {
                        jumpPoints.add(jumpPoint);
                    }
                }

                if (!jumpPoints.isEmpty()) {
                    jumpPointsMap.put(start, jumpPoints);
                }
            }
        }
    }

    private boolean isNearObstacleOrBoundary(int x, int y) {
        // Check if node is near grid boundary
        if (x <= 1 || x >= grid.width - 2 || y <= 1 || y >= grid.height - 2) {
            return true;
        }

        // Check if node is near an obstacle (8-connected neighborhood)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                if (!grid.isWalkable(x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAdjacentObstacle(int x, int y, Direction dir) {
        // Check perpendicular directions for obstacles
        int perpX = dir.dy; // Perpendicular direction
        int perpY = -dir.dx;

        return !grid.isWalkable(x + perpX, y + perpY) ||
                !grid.isWalkable(x - perpX, y - perpY);
    }

    // Find the jump point in a given direction
    private JpsNode findJumpPoint(JpsNode start, Direction dir, JpsGrid grid) {
        int x = start.x;
        int y = start.y;

        while (grid.isWalkable(x, y)) {
            x += dir.dx;
            y += dir.dy;

            // Stop if out of grid boundaries`
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
        Map<String, JpsNode> nodeMap = new HashMap<>();  // Keep track of actual nodes
        Set<String> closedSet = new HashSet<>();
        PriorityQueue<JpsNode> openList = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));

        start.g = 0;
        start.f = heuristic(start, goal);
        openList.add(start);
        nodeMap.put(nodeKey(start), start);

        while (!openList.isEmpty()) {
            JpsNode current = openList.poll();
            String currentKey = nodeKey(current);

            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            if (closedSet.contains(currentKey)) {
                continue;
            }
            closedSet.add(currentKey);

            // First, check precomputed jump points
            List<JpsNode> jumpPoints = jumpPointsMap.getOrDefault(current, Collections.emptyList());
            for (JpsNode jumpPoint : jumpPoints) {
                processSuccessor(jumpPoint, current, goal, openList, closedSet, nodeMap);
            }

            // Then process natural neighbors
            processNaturalNeighbors(current, goal, openList, closedSet, nodeMap);
        }

        return null;
    }

    private void processNaturalNeighbors(JpsNode current, JpsNode goal,
                                         PriorityQueue<JpsNode> openList,
                                         Set<String> closedSet,
                                         Map<String, JpsNode> nodeMap) {
        // Straight directions
        int[][] straight = {{0,1}, {1,0}, {0,-1}, {-1,0}};
        // Diagonal directions
        int[][] diagonal = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};

        // Process straight neighbors
        for (int[] dir : straight) {
            int newX = current.x + dir[0];
            int newY = current.y + dir[1];

            if (!grid.isWalkable(newX, newY)) {
                continue;
            }

            JpsNode neighbor = new JpsNode(newX, newY);
            processSuccessor(neighbor, current, goal, openList, closedSet, nodeMap);
        }

        // Process diagonal neighbors
        for (int[] dir : diagonal) {
            int newX = current.x + dir[0];
            int newY = current.y + dir[1];

            if (!grid.isWalkable(newX, newY)) {
                continue;
            }

            // Check if we can move diagonally (both adjacent cells must be walkable)
            if (!grid.isWalkable(current.x + dir[0], current.y)  ||
                    !grid.isWalkable(current.x, current.y + dir[1])) {
                continue;
            }

            JpsNode neighbor = new JpsNode(newX, newY);
            processSuccessor(neighbor, current, goal, openList, closedSet, nodeMap);
        }
    }

    private void processSuccessor(JpsNode successor, JpsNode current, JpsNode goal,
                                  PriorityQueue<JpsNode> openList,
                                  Set<String> closedSet,
                                  Map<String, JpsNode> nodeMap) {
        String successorKey = nodeKey(successor);

        if (closedSet.contains(successorKey)) {
            return;
        }

        double tentativeG = current.g + distance(current, successor);

        JpsNode existingNode = nodeMap.get(successorKey);
        if (existingNode == null) {
            successor.g = tentativeG;
            successor.f = successor.g + heuristic(successor, goal);
            successor.parent = current;
            openList.add(successor);
            nodeMap.put(successorKey, successor);
        } else if (tentativeG < existingNode.g) {
            existingNode.g = tentativeG;
            existingNode.f = existingNode.g + heuristic(existingNode, goal);
            existingNode.parent = current;
            // Re-add to update position in priority queue
            openList.remove(existingNode);
            openList.add(existingNode);
        }
    }

    private String nodeKey(JpsNode node) {
        return node.x + "," + node.y;
    }

    private double distance(JpsNode a, JpsNode b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        // Use octile distance for more accurate estimation
        return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
    }

    private double heuristic(JpsNode a, JpsNode b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
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
