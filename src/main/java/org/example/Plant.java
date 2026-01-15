package org.example;

public class Plant {
    private String name;
    private int threshold;

    public Plant(String name, int threshold) {
        this.name = name;
        this.threshold = threshold;
    }

    public String getName() { return name; }
    public int getThreshold() { return threshold; }

    @Override
    public String toString() {
        return name + " (" + threshold + "%)";
    }
}