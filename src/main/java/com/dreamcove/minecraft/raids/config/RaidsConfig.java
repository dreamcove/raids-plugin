package com.dreamcove.minecraft.raids.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class RaidsConfig {
    private final List<Raid> raids = Collections.synchronizedList(new ArrayList<>());

    public static RaidsConfig from(FileConfiguration fileConfig) {
        RaidsConfig result = new RaidsConfig();

        ConfigurationSection section = fileConfig.getConfigurationSection("raids");

        for (String raidName : section.getKeys(false)) {
            try {
                ConfigurationSection raidSection = section.getConfigurationSection(raidName);
                Raid raid = new Raid();

                raid.setName(raidName);
                assert raidSection != null;
                raid.setDungeonName(raidSection.getString("dungeon", "arena"));
                raid.setJoinIn(raidSection.getInt("join-in", 15));
                raid.setSpawnLocation(Point.parse(raidSection.getString("spawn-location", "0, 0, 0")));

                result.addRaid(raid);
            } catch (Throwable t) {
                Logger.getLogger(RaidsConfig.class.getName()).severe("Unable to load raid " + raidName);
                Logger.getLogger(RaidsConfig.class.getName()).throwing("RaidsConfig", "parse", t);
            }
        }

        return result;
    }

    public void addRaid(Raid raid) {
        raids.add(raid);
    }

    public List<Raid> getRaids() {
        return raids;
    }
}
