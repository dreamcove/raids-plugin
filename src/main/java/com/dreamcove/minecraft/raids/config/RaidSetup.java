package com.dreamcove.minecraft.raids.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RaidSetup {
    private final List<String> commands = Collections.synchronizedList(new ArrayList<>());
    private final List<Mob> mobs = Collections.synchronizedList(new ArrayList<>());
    private boolean clearMobs;

    public boolean isClearMobs() {
        return clearMobs;
    }

    public void setClearMobs(boolean clearMobs) {
        this.clearMobs = clearMobs;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void addCommand(String command) {
        commands.add(command);
    }

    public List<Mob> getMobs() {
        return mobs;
    }

    public void addMob(Mob mob) {
        mobs.add(mob);
    }
}
