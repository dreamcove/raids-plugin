package com.dreamcove.minecraft.raids.config;

public class Mob {
    private final String type;
    private final Point location;

    public Mob(String type, Point location) {
        this.type = type;
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public Point getLocation() {
        return location;
    }


}
