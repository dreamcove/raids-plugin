package com.dreamcove.minecraft.raids.impl;

import com.dreamcove.minecraft.raids.api.*;
import com.dreamcove.minecraft.raids.config.Point;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
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

    private Location toLocation(WorldLocation worldLoc) {
        return new Location(
                ((PluginWorld) worldLoc.getWorld()).getWorld(),
                worldLoc.getPoint().getX(),
                worldLoc.getPoint().getY(),
                worldLoc.getPoint().getZ()
        );
    }

    private WorldLocation toWorldLocation(Location location) {
        return new WorldLocation(
                new PluginWorld(
                        location.getWorld()),
                new Point(
                        location.getX(),
                        location.getY(),
                        location.getZ()));
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
        public WorldLocation getLocation() {
            return toWorldLocation(player.getLocation());
        }

        @Override
        public void teleport(WorldLocation location) {
            player.teleport(toLocation(location));
        }

        @Override
        public World getWorld() {
            return new PluginWorld(player.getWorld());
        }

        @Override
        public int getLevel() {
            return player.getLevel();
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

        @Override
        public String getName() {
            return world.getName();
        }

        public File getWorldFolder() {
            return world.getWorldFolder();
        }

        @Override
        public WorldLocation getSpawnLocation() {
            return toWorldLocation(world.getSpawnLocation());
        }

        @Override
        public void setSpawnLocation(WorldLocation location) {
            world.setSpawnLocation(toLocation(location));
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

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PluginWorld) {
                return ((PluginWorld) obj).getWorld().equals(world);
            }

            return false;
        }

        protected org.bukkit.World getWorld() {
            return world;
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

            w.setDifficulty(Difficulty.PEACEFUL);
            w.getWorld().setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.getWorld().setDifficulty(Difficulty.NORMAL);

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
