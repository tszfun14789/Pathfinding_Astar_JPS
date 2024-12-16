package com.example.pathfinding;

class JpsGrid {
    int width, height;
    boolean[][] walkable;

    public JpsGrid(int width, int height) {
        this.width = width;
        this.height = height;
        walkable = new boolean[width][height];
    }

    public boolean isWalkable(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && walkable[x][y];
    }

    public boolean hasForcedNeighbor(int x, int y, Direction dir) {
        // Simplified logic for forced neighbors
        return false;
    }
}
