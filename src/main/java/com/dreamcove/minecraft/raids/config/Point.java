package com.dreamcove.minecraft.raids.config;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Point {

    private final int x;
    private final int y;
    private final int z;
    public Point(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Point parse(String stringLoc) throws ParseException {
        String[] parts = stringLoc.split(",");

        if (parts.length == 3) {
            List<Integer> ints = Stream.of(parts)
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            return new Point(ints.get(0), ints.get(1), ints.get(2));
        }

        throw new ParseException("Unknown format for point", 0);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
