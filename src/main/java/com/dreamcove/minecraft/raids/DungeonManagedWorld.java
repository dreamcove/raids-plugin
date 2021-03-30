package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.World;

public class DungeonManagedWorld extends ManagedWorld {
    private final String dungeon;

    public DungeonManagedWorld(World world, String dungeon) {
        super(world);

        this.dungeon = dungeon;
    }

    public String getDungeon() {
        return dungeon;
    }
}
