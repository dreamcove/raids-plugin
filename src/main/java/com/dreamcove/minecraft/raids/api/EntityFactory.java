package com.dreamcove.minecraft.raids.api;

public abstract class EntityFactory {

    public abstract Server getServer();

    public abstract Player wrap(org.bukkit.entity.Player player);

    public static void setInstance(EntityFactory pInstance) {
        instance = pInstance;
    }

    public static EntityFactory getInstance() {
        return instance;
    }

    private static EntityFactory instance;
}
