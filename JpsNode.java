package com.example.pathfinding;

import java.util.Objects;

class JpsNode implements Comparable<JpsNode> {
    int x, y;
    double g, f;
    JpsNode parent;

    public JpsNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int compareTo(JpsNode other) {
        return Double.compare(this.f, other.f);
    }
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpsNode node = (JpsNode) o;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
