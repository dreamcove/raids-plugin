package com.dreamcove.minecraft.raids.api;

import com.dreamcove.minecraft.raids.RaidsException;
import com.dreamcove.minecraft.raids.config.Point;

import java.text.ParseException;

public class WorldLocation {

    private final Point point;
    private final World world;

    public WorldLocation(World world, Point point) {
        this.world = world;
        this.point = point;
    }

    public static WorldLocation parse(String string) throws ParseException, RaidsException {
        String[] parts = string.split(":");

        if (parts.length == 2) {
            String worldName = parts[0];
            Point point = Point.parse(parts[1]);

            World world = EntityFactory.getInstance().getServer().getWorld(worldName);

            if (world == null) {
                throw new RaidsException("World " + worldName + " does not exist");
            }

            return new WorldLocation(world, point);
        }

        throw new ParseException("wrong format for WorldLocation", 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WorldLocation) {
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
        return getWorld().getName() + ":" + point.toString();
    }

    public World getWorld() {
        return world;
    }

    public Point getPoint() {
        return point;
    }
}
