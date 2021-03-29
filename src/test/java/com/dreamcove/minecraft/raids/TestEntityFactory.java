package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.Player;
import com.dreamcove.minecraft.raids.api.Server;
import com.dreamcove.minecraft.raids.api.World;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
        private Location location = new Location(null, 0, 0, 0);
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
        public Location getLocation() {
            return location;
        }

        @Override
        public void teleport(Location location) {
            this.location = location;
        }

        @Override
        public World getWorld() {
            return world;
        }

        public void setWorld(World world) {
            this.world = world;
        }
    }

    class TestWorld implements World {

        private final String name;
        private final List<Player> players = new ArrayList<>();
        private Difficulty difficulty;

        public TestWorld(String name) {
            this.name = name;
        }

        public void addPlayer(Player player) {
            players.add(player);
        }

        public void clearPlayers() {
            players.clear();
        }        @Override
        public String getName() {
            return name;
        }




        @Override
        public File getWorldFolder() {
            return new File("test-data", getName());
        }


        @Override
        public Location getSpawnLocation() {
            return new Location(null, 10, 10, 10);
        }


        @Override
        public List<Player> getPlayers() {
            return players;
        }


        @Override
        public List<Entity> getEntities() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
        }

        @Override
        public void removeAllEntities() {
            // Not implemented
        }


    }

    class TestServer implements Server {

        List<Player> players = new ArrayList<Player>();
        List<World> worlds = new ArrayList<World>();

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
            return new ArrayList<World>(worlds);
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

            return w;
        }

        @Override
        public File getWorldContainer() {
            return new File("test-data");
        }

        @Override
        public void delayRunnable(Runnable runnable, long ticks) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(1000 * (ticks / 20));
                        runnable.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }

        @Override
        public void scheduleRunnable(Runnable runnable, long everyTicks) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(1000 * (everyTicks / 20));
                        runnable.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }


    }
}
