package com.dreamcove.minecraft.raids.config;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Point {

    private final double x;
    private final double y;
    private final double z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Point parse(String stringLoc) throws ParseException {
        String[] parts = stringLoc.split(",");

        if (parts.length == 3) {
            List<Double> doubles = Stream.of(parts)
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());

            return new Point(doubles.get(0), doubles.get(1), doubles.get(2));
        }

        throw new ParseException("Unknown format for point", 0);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            return obj.toString().equals(toString());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("%.2f,%.2f,%.2f", x, y, z);
    }
}
