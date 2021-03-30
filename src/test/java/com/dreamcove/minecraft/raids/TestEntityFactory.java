package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import com.dreamcove.minecraft.raids.config.Point;
import org.bukkit.Difficulty;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class TestEntityFactory extends EntityFactory {

    private final TestServer server = new TestServer();

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public Player wrap(org.bukkit.entity.Player player) {
        return null;
    }

    public static class TestPlayer implements Player {

        private final String name;
        private final UUID uniqueId = UUID.randomUUID();
        private WorldLocation location = new WorldLocation(null, new Point(0, 0, 0));
        private World world;

        public TestPlayer(String name) {
            this.name = name;
        }

        @Override
        public void sendMessage(String message) {
            Logger.getLogger("Player-" + getName()).info(message);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public UUID getUniqueId() {
            return uniqueId;
        }

        @Override
        public WorldLocation getLocation() {
            return location;
        }

        @Override
        public void teleport(WorldLocation location) {
            this.location = location;
        }

        @Override
        public World getWorld() {
            return world;
        }

        @Override
        public int getLevel() {
            return 1;
        }

        public void setWorld(World world) {
            this.world = world;
        }
    }

    static class TestWorld implements World {

        private final String name;
        private final List<Player> players = new ArrayList<>();
        private WorldLocation spawnLocation;

        public TestWorld(String name) {
            this.name = name;
        }

        public void addPlayer(Player player) {
            players.add(player);
        }

        public void clearPlayers() {
            players.clear();
        }

        @Override
        public String getName() {
            return name;
        }


        @Override
        public File getWorldFolder() {
            return new File(new File(new File(new File("target"), "test-data"), "worlds"), getName());
        }


        @Override
        public WorldLocation getSpawnLocation() {
            if (spawnLocation == null) {
                spawnLocation = new WorldLocation(this, new Point(10, 10, 10));
            }
            return spawnLocation;
        }

        @Override
        public void setSpawnLocation(WorldLocation location) {
            this.spawnLocation = location;
        }


        @Override
        public List<Player> getPlayers() {
            return players;
        }


        @Override
        public void setDifficulty(Difficulty difficulty) {
            // Not implemented
        }

        @Override
        public void spawnEntity(EntityType type, double x, double y, double z) {
            // Not implemented
        }

        @Override
        public void removeAllEntities() {
            // Not implemented
        }


    }

    static class TestServer implements Server {

        List<Player> players = new ArrayList<>();
        List<World> worlds = new ArrayList<>();

        protected void addPlayer(Player player) {
            players.add(player);
        }

        @Override
        public World getWorld(String name) {
            return getWorlds().stream()
                    .filter(w -> w.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }


        @Override
        public List<World> getWorlds() {
            return new ArrayList<>(worlds);
        }


        @Override
        public boolean unloadWorld(String worldName) {
            World w = getWorld(worldName);

            if (w != null) {
                worlds.remove(w);
            }

            return true;
        }

        @Override
        public Player getPlayer(String name) {
            return players.stream()
                    .filter(p -> p.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Player getPlayer(UUID uuid) {
            return players.stream()
                    .filter(p -> p.getUniqueId().equals(uuid))
                    .findFirst()
                    .orElse(null);
        }


        @Override
        public World createWorld(WorldCreator creator) {
            World w = new TestWorld(creator.name());
            worlds.add(w);

            w.getWorldFolder().mkdirs();

            return w;
        }

        @Override
        public File getWorldContainer() {
            return new File(new File(new File("target"), "test-data"), "worlds");
        }

        @Override
        public void delayRunnable(Runnable runnable, long ticks) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1000 * (ticks / 20));
                    runnable.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            t.start();
        }

        @Override
        public void dispatchCommand(String command) {
            Logger.getLogger(this.getClass().getName()).info("Dispatching command: " + command);
        }
    }
}
