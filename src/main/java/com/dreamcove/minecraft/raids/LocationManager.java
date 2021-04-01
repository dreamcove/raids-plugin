package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.WorldLocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocationManager {

    private final File locationFile;
    private final Map<UUID, WorldLocation> lastLocations = Collections.synchronizedMap(new HashMap<>());

    public LocationManager(File locationFile) {
        this.locationFile = locationFile;

        initialize();
    }

    private void initialize() {
        if (locationFile.exists()) {
            try {
                loadLocations();
            } catch (IOException | InvalidConfigurationException e) {
                getLogger().log(Level.SEVERE, "Unable to load locations file", e);
            }
        }
    }

    private void loadLocations() throws IOException, InvalidConfigurationException {
        YamlConfiguration configFile = new YamlConfiguration();

        configFile.load(locationFile);

        ConfigurationSection section = configFile.getConfigurationSection("locations");

        if (section != null) {
            Set<String> keys = section.getKeys(false);

            for (String key : keys) {
                String location = section.getString(key);

                if (location != null) {
                    try {
                        WorldLocation worldLoc = WorldLocation.parse(location);

                        lastLocations.put(UUID.fromString(key), worldLoc);
                    } catch (ParseException | RaidsException e) {
                        getLogger().log(Level.WARNING, "Error loading player " + key, e);
                    }
                }
            }
        }
    }

    private Logger getLogger() {
        return Logger.getLogger(LocationManager.class.getName());
    }

    public void store(UUID player, WorldLocation location) {
        lastLocations.put(player, location);

        try {
            saveLocations();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error saving locations", e);
        }
    }

    private void saveLocations() throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        synchronized (lastLocations) {
            lastLocations.keySet().forEach(k -> {
                config.set("locations." + k.toString(), lastLocations.get(k).toString());
            });
        }
        config.save(locationFile);
    }

    public WorldLocation get(UUID player) {
        return lastLocations.get(player);
    }

    public WorldLocation remove(UUID player) {
        WorldLocation result = lastLocations.remove(player);

        if (result != null) {
            try {
                saveLocations();
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Error saving locations", e);
            }
        }

        return result;
    }
}
