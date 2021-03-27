package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * RaidsManager handles the logic around the management of the raid.
 *
 * @author vaporofnuance
 */
public class RaidsManager {

    // Constants
    public static final String PERM_RELOAD = "raids.reload";
    public static final String PERM_START_RAID = "raids.start";
    public static final String PERM_CANCEL_RAID = "raids.cancel";
    public static final String PERM_END_RAID = "raids.end";
    public static final String PERM_EXIT_RAID = "raids.exit";

    // Constructors
    public RaidsManager(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    // Instance Methods
    public final List<String> getHelp(List<String> perms) {
        ArrayList<String> result = new ArrayList<String>();

        if (perms.contains(PERM_START_RAID)) {
            result.add("/raids start <raid> - Start specified raid");
        }

        if (perms.contains(PERM_CANCEL_RAID)) {
            result.add("/raids cancel - Cancel raid before it starts");
        }

        if (perms.contains(PERM_END_RAID)) {
            result.add("/raids end - Ends the raid for all members of the party");
        }

        if (perms.contains(PERM_EXIT_RAID)) {
            result.add("/raids exit - Exit the raid (just you)");
        }

        if (perms.contains(PERM_RELOAD)) {
            result.add("/raids reload - Reload config for plugin");
        }

        return result;
    }

    public void storeLastLocation(Player player) {
        lastLocation.put(player.getUniqueId(), player.getLocation());
    }

    public void returnLastLocation(Player player) {
        if (lastLocation.get(player.getUniqueId()) != null) {
            player.teleport(lastLocation.get(player.getUniqueId()));
            lastLocation.remove(player.getUniqueId());
        }
    }

    public Location getLastLocation(Player player) {
        return lastLocation.get(player.getUniqueId());
    }

    public List<String> getAvailableRaids() {
        if (availableRaids == null) {
            availableRaids = Collections.synchronizedList(EntityFactory.getInstance().getServer()
                    .getWorlds()
                    .stream()
                    .filter(w -> w.getName().startsWith("template_"))
                    .map(w -> w.getName().substring(9))
                    .collect(Collectors.toList()));
        }

        return availableRaids;
    }


    public String getQueuedWorld(UUID partyId) {
        return queuedParties.get(partyId);
    }

    public void queueParty(UUID partyId, String worldName) {
        queuedParties.put(partyId, worldName);
    }

    public void dequeueParty(UUID partyId) {
        queuedParties.remove(partyId);
    }

    public boolean isPartyQueued(UUID partyId) {
        return queuedParties.containsKey(partyId);
    }

    public void cleanRaids() {
        for (Iterator<World> i = getServer().getWorlds().iterator(); i.hasNext();) {
            World w = i.next();

            if (!queuedParties.values().contains(w.getName())) {
                if (w.getName().startsWith("raid_")) {
                    if (w.getPlayers().size() == 0) {
                        getLogger().info("Removing unused dungeon - " + w.getName() + " (no players)");

                        try {
                            removeWorld(w);
                        } catch (IOException e) {
                            getLogger().throwing(RaidsPlugin.class.getName(), "onEnable", e);
                        }
                    }
                }
            }
        }
    }

    private void deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i=0; i < files.length; i++) {
                deleteFile(files[i]);
            }
        }

        file.delete();
    }

    public void removeWorld(World world) throws IOException {
        if (EntityFactory.getInstance().getServer().unloadWorld(world.getName())) {
            getLogger().info("Removing raid " + world.getName());
            deleteFile(world.getWorldFolder());
            getLogger().info(world.getName() + " removed.");
        }
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(RaidsManager.class.getName());
        }

        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private Server getServer() {
        return EntityFactory.getInstance().getServer();
    }

    private void copyFile(File fromFile, File toFile) throws IOException {
        if (fromFile.isDirectory()) {
            toFile.mkdirs();

            File[] files = fromFile.listFiles();

            for (int i=0; i < files.length; i++) {
                copyFile(files[i], new File(toFile, files[i].getName()));
            }
        } else if (!fromFile.getName().equals("uid.dat")){
            byte[] buffer = new byte[8192];
            int read;

            InputStream is = new FileInputStream(fromFile);
            OutputStream os = new FileOutputStream(toFile);

            do {
                read = is.read(buffer);
                if (read > 0) {
                    os.write(buffer, 0, read);
                }
            } while (read > 0);

            is.close();
            os.close();

            toFile.setLastModified(fromFile.lastModified());
        }
    }

    public void cloneWorld(String fromWorld, String toWorld) throws IOException {
        // Ensure the from world exists and the to world does not exist
        if (getServer().getWorld(fromWorld) != null && getServer().getWorld(toWorld) == null) {
            getLogger().info("Cloning " + fromWorld + " to create " + toWorld);

            copyFile(getServer().getWorld(fromWorld).getWorldFolder(), new File(getServer().getWorldContainer(), toWorld));

            WorldCreator creator = new WorldCreator(toWorld);
            World newWorld = EntityFactory.getInstance().getServer().createWorld(creator);

            // Turn off animal/monster spawns
            newWorld.setDifficulty(Difficulty.PEACEFUL);


            // clear all existing mobs
            newWorld.getEntities().stream().forEach(e -> e.remove());

            getLogger().info("Clone complete.");
        }
    }


    private List<String> availableRaids = null;
    private Map<UUID, String> queuedParties = Collections.synchronizedMap(new HashMap<UUID, String>());
    private Map<UUID, Location> lastLocation = Collections.synchronizedMap(new HashMap<UUID, Location>());
    private FileConfiguration config;
    private Logger logger;
}
