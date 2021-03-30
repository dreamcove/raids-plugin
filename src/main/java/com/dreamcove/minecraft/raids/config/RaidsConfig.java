package com.dreamcove.minecraft.raids.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class RaidsConfig {
    private final List<Raid> raids = Collections.synchronizedList(new ArrayList<>());
    private int cleanCycle = 15;
    private String raidWorldPrefix = "partyraids";

    public static RaidsConfig from(FileConfiguration fileConfig) {
        RaidsConfig result = new RaidsConfig();

        result.setCleanCycle(fileConfig.getInt("clean-cycle", 15));
        result.setRaidWorldPrefix(fileConfig.getString("raid-world-prefix", "partyraids"));

        ConfigurationSection section = fileConfig.getConfigurationSection("raids");

        for (String raidName : section.getKeys(false)) {
            try {
                ConfigurationSection raidSection = section.getConfigurationSection(raidName);

                Raid raid = new Raid();

                raid.setName(raidName);
                assert raidSection != null;
                raid.setDungeonName(raidSection.getString("dungeon", "arena"));
                raid.setDifficulty(raidSection.getString("difficulty", "normal"));
                raid.setJoinIn(raidSection.getInt("join-in", 15));
                raid.setSpawnLocation(Point.parse(raidSection.getString("spawn-location", "0, 0, 0")));

                if (raidSection.getKeys(false).contains("on-startup")) {
                    ConfigurationSection onStartUpSection = raidSection.getConfigurationSection("on-startup");

                    raid.getOnStartup().setClearMobs(onStartUpSection.getBoolean("clear-mobs", true));
                    onStartUpSection.getStringList("commands").forEach(raid.getOnStartup()::addCommand);

                    ConfigurationSection mobs = onStartUpSection.getConfigurationSection("mobs");

                    if (mobs != null) {
                        for (String key : mobs.getKeys(false)) {
                            ConfigurationSection mobSection = mobs.getConfigurationSection(key);

                            String mobType = mobSection.getString("type");
                            Point location = Point.parse(mobSection.getString("location"));
                            Mob mob = new Mob(mobType, location);

                            raid.getOnStartup().addMob(mob);
                        }
                    }
                }

                if (raidSection.getKeys(false).contains("join-criteria")) {
                    ConfigurationSection joinSection = raidSection.getConfigurationSection("join-criteria");

                    raid.getJoinCriteria().setMinimumLevel(joinSection.getInt("minimum-rank"));
                    raid.getJoinCriteria().setMinimumPartySize(joinSection.getInt("minimum-party-size"));
                }

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

    public String getRaidWorldPrefix() {
        return raidWorldPrefix;
    }

    public void setRaidWorldPrefix(String raidWorldPrefix) {
        this.raidWorldPrefix = raidWorldPrefix;
    }

    public int getCleanCycle() {
        return cleanCycle;
    }

    public void setCleanCycle(int cleanCycle) {
        this.cleanCycle = cleanCycle;
    }

    public List<Raid> getRaids() {
        return raids;
    }
}
