package com.dreamcove.minecraft.raids.api;

import com.dreamcove.minecraft.raids.config.Point;

public class WorldLocation {
    private final Point point;
    private final World world;

    public WorldLocation(World world, Point point) {
        this.world = world;
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }

    public World getWorld() {
        return world;
    }
}
