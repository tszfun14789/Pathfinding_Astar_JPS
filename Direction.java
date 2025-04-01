package com.example.pathfinding;

// Direction.java
public enum Direction {
    N(0, -1), S(0, 1), E(1, 0), W(-1, 0),
    NE(1, -1), NW(-1, -1), SE(1, 1), SW(-1, 1);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
    public boolean isDiagonal() {
        return dx != 0 && dy != 0;
    }
}
