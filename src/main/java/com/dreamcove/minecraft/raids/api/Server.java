package com.dreamcove.minecraft.raids.api;

import org.bukkit.WorldCreator;

import java.io.File;
import java.util.List;
import java.util.UUID;

public interface Server {
    World getWorld(String name);

    List<World> getWorlds();

    boolean unloadWorld(String worldName);

    Player getPlayer(String name);

    Player getPlayer(UUID uuid);

    World createWorld(WorldCreator creator);

    File getWorldContainer();

    void delayRunnable(Runnable runnable, long ticks);

    void dispatchCommand(String command);
}
