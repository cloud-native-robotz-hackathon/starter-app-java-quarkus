package com.redhat.robotics.model;

public record Coordinates(
    double confidenceScore,
    double xUpperLeft,
    double yUpperLeft,
    double xLowerRight,
    double yLowerRight,
    int objectClass
) {
    public double getCenterX() {
        return (xUpperLeft + xLowerRight) / 2;
    }
    
    public double getDelta() {
        return xLowerRight - xUpperLeft;
    }
}
