package com.dreamcove.minecraft.raids.api;

import org.bukkit.Location;

import java.util.UUID;

public interface Player extends MessageReceiver {

    String getName();

    UUID getUniqueId();

    Location getLocation();

    void teleport(Location location);

    World getWorld();
}
