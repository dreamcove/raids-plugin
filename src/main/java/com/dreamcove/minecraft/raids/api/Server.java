package com.dreamcove.minecraft.raids.api;

import org.bukkit.WorldCreator;

import java.io.File;
import java.util.List;

public interface Server {
    World getWorld(String name);

    List<World> getWorlds();

    boolean unloadWorld(String worldName);

    Player getPlayer(String name);

    World createWorld(WorldCreator creator);

    File getWorldContainer();
}
