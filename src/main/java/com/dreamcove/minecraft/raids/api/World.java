package com.dreamcove.minecraft.raids.api;

import org.bukkit.Difficulty;
import org.bukkit.Location;

import java.io.File;
import java.util.List;

public interface World {

    String getName();

    File getWorldFolder();

    Location getSpawnLocation();

    List<Player> getPlayers();

    List<org.bukkit.entity.Entity> getEntities();

    void setDifficulty(Difficulty difficulty);

    void removeAllEntities();
}
