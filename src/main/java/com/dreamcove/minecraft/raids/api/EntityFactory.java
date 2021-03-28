package com.dreamcove.minecraft.raids.api;

public abstract class EntityFactory {

    private static EntityFactory instance;

    public static EntityFactory getInstance() {
        return instance;
    }

    public static void setInstance(EntityFactory pInstance) {
        instance = pInstance;
    }

    public abstract Server getServer();

    public abstract Player wrap(org.bukkit.entity.Player player);
}
