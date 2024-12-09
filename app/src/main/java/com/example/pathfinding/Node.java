package com.example.pathfinding;

import java.util.Objects;

public class Node implements Comparable<Node> {
    public int x, y;
    public int gCost = Integer.MAX_VALUE;
    public int hCost = 0;
    public Node parent;
    public boolean visited = false;

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getFCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.getFCost(), other.getFCost());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
