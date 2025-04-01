package com.example.pathfinding;

class JpsGrid {
    int width, height;
    boolean[][] walkable;

    public JpsGrid(int width, int height) {
        this.width = width;
        this.height = height;
        walkable = new boolean[width][height];
    }
    public JpsGrid(int[][] grid) {
        // Assuming input grid has rows as height and columns as width
        this.height = grid.length; // Number of rows
        this.width = grid[0].length; // Number of columns
        walkable = new boolean[width][height]; // Transpose for proper orientation

        // Convert the int grid to the boolean walkable grid
        for (int y = 0; y < height; y++) { // Iterate rows
            for (int x = 0; x < width; x++) { // Iterate columns
                walkable[x][y] = grid[y][x] == 0; // Transpose during assignment
            }
        }
    }

    public boolean isWalkable(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && walkable[x][y];
    }
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean hasForcedNeighbor(int x, int y, Direction dir) {
        // Ensure the current position is within bounds and walkable
        if (!isInBounds(x, y) || !isWalkable(x, y)) {
            return false;
        }

        // Forced neighbor detection for 8 directions
        switch (dir) {
            case N: // North
                return (!isWalkable(x - 1, y) && isWalkable(x - 1, y - 1)) ||
                        (!isWalkable(x + 1, y) && isWalkable(x + 1, y - 1));
            case S: // South
                return (!isWalkable(x - 1, y) && isWalkable(x - 1, y + 1)) ||
                        (!isWalkable(x + 1, y) && isWalkable(x + 1, y + 1));
            case E: // East
                return (!isWalkable(x, y - 1) && isWalkable(x + 1, y - 1)) ||
                        (!isWalkable(x, y + 1) && isWalkable(x + 1, y + 1));
            case W: // West
                return (!isWalkable(x, y - 1) && isWalkable(x - 1, y - 1)) ||
                        (!isWalkable(x, y + 1) && isWalkable(x - 1, y + 1));
            case NE: // Northeast
                return (!isWalkable(x, y - 1) && isWalkable(x + 1, y - 1)) ||
                        (!isWalkable(x + 1, y) && isWalkable(x + 1, y - 1));
            case NW: // Northwest
                return (!isWalkable(x, y - 1) && isWalkable(x - 1, y - 1)) ||
                        (!isWalkable(x - 1, y) && isWalkable(x - 1, y - 1));
            case SE: // Southeast
                return (!isWalkable(x, y + 1) && isWalkable(x + 1, y + 1)) ||
                        (!isWalkable(x + 1, y) && isWalkable(x + 1, y + 1));
            case SW: // Southwest
                return (!isWalkable(x, y + 1) && isWalkable(x - 1, y + 1)) ||
                        (!isWalkable(x - 1, y) && isWalkable(x - 1, y + 1));
        }

        return false; // Default case: no forced neighbors
    }

}
