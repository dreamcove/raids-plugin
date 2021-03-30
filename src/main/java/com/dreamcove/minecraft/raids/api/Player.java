package com.dreamcove.minecraft.raids.api;

import java.util.UUID;

public interface Player extends MessageReceiver {

    String getName();

    UUID getUniqueId();

    WorldLocation getLocation();

    void teleport(WorldLocation location);

    World getWorld();

    int getLevel();
}
