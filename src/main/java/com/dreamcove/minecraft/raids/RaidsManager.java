package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;

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
    public static final String CMD_RELOAD = "reload";
    public static final String CMD_START = "start";
    public static final String CMD_CANCEL = "cancel";
    public static final String CMD_END = "end";
    public static final String CMD_EXIT = "exit";
    public static final String CMD_HELP = "help";
    public static final String PERM_RELOAD = "raids.reload";
    public static final String PERM_START_RAID = "raids.start";
    public static final String PERM_CANCEL_RAID = "raids.cancel";
    public static final String PERM_END_RAID = "raids.end";
    public static final String PERM_EXIT_RAID = "raids.exit";
    private final Map<UUID, String> queuedParties = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Location> lastLocation = Collections.synchronizedMap(new HashMap<>());
    private List<String> availableRaids = null;
    private Logger logger;

    // Constructors
    public RaidsManager(Logger logger) {
        this.logger = logger;
    }

    // Instance Methods
    public final List<String> getHelp(List<String> perms) {
        ArrayList<String> result = new ArrayList<>();

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

    public List<String> getTabComplete(String command, List<String> perms, List<String> args) {
        List<String> result = new ArrayList<>();

        if (command.equals("raids")) {
            if (args.size() == 1) {
                result.add("CMD_HELP");
                if (perms.contains(PERM_START_RAID)) {
                    result.add(CMD_START);
                }
                if (perms.contains(PERM_CANCEL_RAID)) {
                    result.add(CMD_CANCEL);
                }
                if (perms.contains(PERM_EXIT_RAID)) {
                    result.add(CMD_EXIT);
                }
                if (perms.contains(PERM_END_RAID)) {
                    result.add(CMD_END);
                }
                if (perms.contains(PERM_RELOAD)) {
                    result.add(CMD_RELOAD);
                }
            } else if (args.size() == 2 && args.get(0).equals(CMD_START) && perms.contains(PERM_START_RAID)) {
                result.addAll(getAvailableRaids());
            }
        }

        return result;
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
        for (World w : getServer().getWorlds()) {
            if (!queuedParties.containsValue(w.getName()) && w.getName().startsWith("raid_") && w.getPlayers().isEmpty()) {
                getLogger().info("Removing unused dungeon - " + w.getName() + " (no players)");

                removeWorld(w);
            }
        }
    }

    public boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            assert files != null;
            for (File value : files) {
                if (!deleteFile(value)) {
                    return false;
                }
            }
        }

        return file.delete();
    }

    public void removeWorld(World world) {
        if (EntityFactory.getInstance().getServer().unloadWorld(world.getName())) {
            getLogger().info("Removing raid " + world.getName());
            if (!deleteFile(world.getWorldFolder())) {
                getLogger().warning("Unable to remove " + world.getName());
            } else {
                getLogger().info(world.getName() + " removed.");
            }
        }
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(RaidsManager.class.getName());
        }

        return logger;
    }

    private Server getServer() {
        return EntityFactory.getInstance().getServer();
    }

    private void copyFile(File fromFile, File toFile) throws IOException {
        if (fromFile.isDirectory()) {
            toFile.mkdirs();

            File[] files = fromFile.listFiles();

            assert files != null;
            for (File file : files) {
                copyFile(file, new File(toFile, file.getName()));
            }
        } else if (!fromFile.getName().equals("uid.dat")) {
            byte[] buffer = new byte[8192];
            int read;

            InputStream is = null;
            OutputStream os = null;

            try {
                is = new FileInputStream(fromFile);
                try {
                    os = new FileOutputStream(toFile);

                    do {
                        read = is.read(buffer);
                        if (read > 0) {
                            os.write(buffer, 0, read);
                        }
                    } while (read > 0);
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
            } finally {
                if (is != null) {
                    is.close();
                }
            }
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
            newWorld.getEntities().forEach(Entity::remove);

            getLogger().info("Clone complete.");
        }
    }

    public boolean cancelRaid(UUID partyId) {
        if (isPartyQueued(partyId)) {
            dequeueParty(partyId);

            PartyFactory.getInstance().getParty(partyId).broadcastMessage("Raid canceled");

            return true;
        }

        return false;
    }

    public void startRaid(UUID partyId, String oldWorld, String newWorld) throws IOException {
        queueParty(partyId, newWorld);

        try {
            cloneWorld(oldWorld, newWorld);

            Party party = PartyFactory.getInstance().getParty(partyId);

            party.broadcastMessage("Party is queued for a raid.");
            party.broadcastMessage("Starting in 15 seconds.");
            party.broadcastMessage("Use /raids cancel to abort.");

            EntityFactory.getInstance().getServer().delayRunnable(new StartRaidRunnable(partyId), (long) 15 * 20);
        } catch (IOException e) {
            dequeueParty(partyId);
            throw e;
        }
    }

    public boolean processCommand(MessageReceiver receiver, String command, List<String> args, List<String> perms) {
        if (command.equals("raids")) {
            if (args.size() >= 1) {
                Player player;

                if (receiver instanceof Player) {
                    player = (Player) receiver;
                } else {
                    player = EntityFactory.getInstance().getServer().getPlayer(args.get(args.size() - 1));

                    if (player == null) {
                        receiver.sendMessage("Player not specified as last parameter");
                    }
                }

                if (player != null) {
                    switch (args.get(0)) {
                        case CMD_HELP:
                            for (String help : getHelp(perms)) {
                                receiver.sendMessage(help);
                            }
                        case CMD_START:
                            if (args.size() == 2) {
                                // Find Party
                                final UUID playerId = player.getUniqueId();
                                final UUID partyId = PartyFactory.getInstance().getPartyForPlayer(playerId);
                                if (partyId == null) {
                                    receiver.sendMessage("Player must belong to party");
                                } else {
                                    boolean found = false;
                                    for (World w : EntityFactory.getInstance().getServer().getWorlds()) {
                                        if (w.getName().equals("template_" + args.get(1))) {
                                            receiver.sendMessage("Creating raid dungeon");

                                            String newWorld = "raid_" + args.get(1) + "_" + System.currentTimeMillis();

                                            try {
                                                cloneWorld(w.getName(), newWorld);
                                                startRaid(partyId, args.get(1), newWorld);
                                                found = true;
                                            } catch (IOException e) {
                                                receiver.sendMessage("Error creating raid");
                                                getLogger().throwing(RaidsPlugin.class.getName(), "onCommand", e);
                                            }

                                        }
                                    }

                                    if (!found) {
                                        receiver.sendMessage("Could not find raid template");
                                    }
                                }
                            } else {
                                receiver.sendMessage("/raids start requires 2 arguments");
                            }
                            break;
                        case CMD_CANCEL:
                            // Check to see if calling entity is a player
                            if (receiver instanceof Player) {
                                final UUID playerId = player.getUniqueId();
                                List<Party> foundParties = PartyFactory.getInstance().getOnlineParties()
                                        .stream()
                                        .filter(p -> p.getMembers().contains(playerId))
                                        .collect(Collectors.toList());

                                if (foundParties.size() > 0) {
                                    if (!cancelRaid(foundParties.get(0).getId())) {
                                        receiver.sendMessage("Your party is not starting a raid");
                                    }
                                } else {
                                    receiver.sendMessage("You must belong to party in order cancel a raid");
                                }
                            } else {
                                receiver.sendMessage("Only players can cancel raids");
                            }
                            break;
                        case CMD_EXIT:
                            returnLastLocation(player);
                            break;
                        case CMD_END:
                            if (getLastLocation(player) != null) {
                                player
                                        .getWorld()
                                        .getPlayers()
                                        .forEach(this::returnLastLocation);
                            }
                            break;
                    }
                }
            } else {
                receiver.sendMessage("Command /raids requires at least one parameter");
            }
            return true;
        }

        return false;
    }

    class StartRaidRunnable implements Runnable {
        UUID partyId;

        public StartRaidRunnable(UUID partyId) {
            super();

            this.partyId = partyId;
        }

        @Override
        public void run() {
            String worldName = getQueuedWorld(partyId);
            if (worldName != null) {
                Party party = PartyFactory.getInstance().getParty(partyId);

                getLogger().info("Sending party " + party.getName() + " to " + getQueuedWorld(partyId));

                World world = EntityFactory.getInstance().getServer().getWorld(getQueuedWorld(partyId));

                party.getMembers().stream()
                        .map(u -> getServer().getPlayer(u))
                        .forEach(p -> {
                            storeLastLocation(p);
                            p.teleport(world.getSpawnLocation());
                        });

                dequeueParty(partyId);
            }
        }
    }
}
