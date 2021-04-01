package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.World;

public abstract class ManagedWorld {

    private final long createdAt;
    private final World world;

    public ManagedWorld(World world) {
        this.world = world;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() {
        return getWorld().getName();
    }

    public World getWorld() {
        return world;
    }

    public final boolean isActive() {
        return !isExpired();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 10000 && world.getPlayers().isEmpty();
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
