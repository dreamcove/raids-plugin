package com.dreamcove.minecraft.raids.impl;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.Player;
import com.dreamcove.minecraft.raids.api.Server;
import com.dreamcove.minecraft.raids.api.World;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PluginEntityFactory extends EntityFactory {

    private final JavaPlugin plugin;

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

    private class PluginPlayer implements Player {
        org.bukkit.entity.Player player;

        PluginPlayer(org.bukkit.entity.Player player) {
            this.player = player;
        }

        @Override
        public String getName() {
            return player.getName();
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

        @Override
        public void sendMessage(String message) {
            player.sendMessage(message);
        }
    }

    private class PluginWorld implements World {
        private final org.bukkit.World world;

        PluginWorld(org.bukkit.World world) {
            this.world = world;
        }

        protected org.bukkit.World getWorld() {
            return world;
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

        @Override
        public Entity spawnEntity(EntityType type, double x, double y, double z) {
            return world.spawnEntity(new Location(world, x, y, z), type);
        }

        @Override
        public void removeAllEntities() {
            world.getEntities().forEach(Entity::remove);
        }
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
                    .map(PluginWorld::new)
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
        public Player getPlayer(UUID uuid) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);

            return p == null ? null : wrap(p);
        }

        @Override
        public World createWorld(WorldCreator creator) {
            PluginWorld w = new PluginWorld(plugin.getServer().createWorld(creator));

            return w;
        }

        @Override
        public File getWorldContainer() {
            return plugin.getServer().getWorldContainer();
        }

        @Override
        public void delayRunnable(final Runnable runnable, long ticks) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskLater(plugin, ticks);
        }

        @Override
        public void dispatchCommand(String command) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }

        @Override
        public void scheduleRunnable(Runnable runnable, long everyTicks) {
            new BukkitRunnable() {

                @Override
                public void run() {

                }
            }.runTaskTimer(plugin, 20, everyTicks);

        }
    }
}
