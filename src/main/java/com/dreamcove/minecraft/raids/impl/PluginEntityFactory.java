package com.dreamcove.minecraft.raids.impl;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.Player;
import com.dreamcove.minecraft.raids.api.Server;
import com.dreamcove.minecraft.raids.api.World;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PluginEntityFactory extends EntityFactory {

    private class PluginPlayer implements Player {
        PluginPlayer(org.bukkit.entity.Player player) {
            this.player = player;
        }

        @Override
        public UUID getUniqueId() {
            return player.getUniqueId();
        }

        @Override
        public Location getLocation() {
            return player.getLocation();
        }

        @Override
        public void teleport(Location location) {
            player.teleport(location);
        }

        @Override
        public World getWorld() {
            return new PluginWorld(player.getWorld());
        }

        org.bukkit.entity.Player player;
    }

    private class PluginWorld implements World {
        PluginWorld(org.bukkit.World world) {
            this.world = world;
        }

        @Override
        public String getName() {
            return world.getName();
        }

        public File getWorldFolder() {
            return world.getWorldFolder();
        }

        @Override
        public Location getSpawnLocation() {
            return world.getSpawnLocation();
        }

        @Override
        public List<Player> getPlayers() {
            return world.getPlayers().stream()
                    .map(p -> EntityFactory.getInstance().wrap(p))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Entity> getEntities() {
            return world.getEntities();
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            world.setDifficulty(difficulty);
        }

        private org.bukkit.World world;
    }

    private class PluginServer implements Server {
        @Override
        public World getWorld(String name) {
            org.bukkit.World w = plugin.getServer().getWorld(name);

            return w == null ? null : new PluginWorld(w);
        }

        @Override
        public List<World> getWorlds() {
            return plugin.getServer().getWorlds().stream()
                    .map(w -> new PluginWorld(w))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean unloadWorld(String worldName) {
            return plugin.getServer().unloadWorld(worldName, true);
        }

        @Override
        public Player getPlayer(String name) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(name);

            return p == null ? null : wrap(p);
        }

        @Override
        public World createWorld(WorldCreator creator) {
            return new PluginWorld(plugin.getServer().createWorld(creator));
        }

        @Override
        public File getWorldContainer() {
            return plugin.getServer().getWorldContainer();
        }
    }

    public PluginEntityFactory(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Server getServer() {
        return new PluginServer();
    }

    @Override
    public Player wrap(org.bukkit.entity.Player player) {
        return new PluginPlayer(player);
    }

    private JavaPlugin plugin;
}
