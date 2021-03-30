package com.dreamcove.minecraft.raids.api;

import org.bukkit.Difficulty;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.List;

public interface World {

    String getName();

    File getWorldFolder();

    WorldLocation getSpawnLocation();

    void setSpawnLocation(WorldLocation location);

    List<Player> getPlayers();

    List<org.bukkit.entity.Entity> getEntities();

    void setDifficulty(Difficulty difficulty);

    Entity spawnEntity(EntityType type, double x, double y, double z);

    void removeAllEntities();
}
