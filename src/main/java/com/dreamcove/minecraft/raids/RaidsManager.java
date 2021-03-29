package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import com.dreamcove.minecraft.raids.config.Raid;
import com.dreamcove.minecraft.raids.config.RaidsConfig;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    public static final List<String> ALL_COMMANDS = Arrays.asList(
            CMD_RELOAD,
            CMD_START,
            CMD_CANCEL,
            CMD_END,
            CMD_EXIT,
            CMD_HELP
    );
    private final Map<UUID, String> queuedParties = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Location> lastLocation = Collections.synchronizedMap(new HashMap<>());
    private final URL configFile;
    private Logger logger;
    private RaidsConfig raidsConfig;
    private boolean running;

    // Constructors
    public RaidsManager(URL configFile, Logger logger) {
        this.logger = logger;
        this.configFile = configFile;
        initialize();
    }

    private void initialize() {
        running = true;
        startScrubber();
    }

    private void startScrubber() {
        EntityFactory.getInstance().getServer().delayRunnable(new RaidScrubber(), getRaidsConfig().getCleanCycle() * 20L);
    }

    public synchronized RaidsConfig getRaidsConfig() {
        if (raidsConfig == null) {
            InputStream is = null;
            try {
                is = configFile.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    result.append(line).append("\n");
                }

                br.close();

                YamlConfiguration yamlConfig = new YamlConfiguration();
                yamlConfig.loadFromString(result.toString());

                raidsConfig = RaidsConfig.from(yamlConfig);
            } catch (Exception ioExc) {
                getLogger().severe("Error loading configuration");
                getLogger().throwing("RaidsManager", "getRaidsConfig", ioExc);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioExc) {
                        ioExc.printStackTrace();
                    }
                }
            }

            if (raidsConfig == null) {
                // Default to an empty config on error
                raidsConfig = new RaidsConfig();
            }
        }

        return raidsConfig;
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(RaidsManager.class.getName());
        }

        return logger;
    }

    public void shutdown() {
        running = false;

        getActiveRaids().stream().forEach(w -> {
            w.getPlayers().forEach(p -> {
                if (getLastLocation(p) != null) {
                    p
                            .getWorld()
                            .getPlayers()
                            .forEach(this::returnLastLocation);
                }
            });
            try {
                removeWorld(w);
            } catch (IOException e) {
                getLogger().severe("Error removing world " + w.getName());
                getLogger().throwing("RaidsManager", "shutdown", e);
            }
        });

        cleanRaids();
    }

    public void reload() {
        raidsConfig = null;
    }

    // Instance Methods
    public final List<String> getHelp(List<String> perms) {
        ArrayList<String> result = new ArrayList<>();

        if (perms.contains(getPermission(CMD_START))) {
            result.add("/raids start <raid> - Start specified raid");
        }

        if (perms.contains(getPermission(CMD_CANCEL))) {
            result.add("/raids cancel - Cancel raid before it starts");
        }

        if (perms.contains(getPermission(CMD_END))) {
            result.add("/raids end - Ends the raid for all members of the party");
        }

        if (perms.contains(getPermission(CMD_EXIT))) {
            result.add("/raids exit - Exit the raid (just you)");
        }

        if (perms.contains(getPermission(CMD_RELOAD))) {
            result.add("/raids reload - Reload config for plugin");
        }

        return result;
    }

    public List<String> getTabComplete(String command, List<String> perms, List<String> args) {
        List<String> result = new ArrayList<>();

        if (command.equals("raids")) {
            if (args.size() == 1) {
                result.add("CMD_HELP");
                if (perms.contains(getPermission(CMD_START))) {
                    result.add(CMD_START);
                }
                if (perms.contains(getPermission(CMD_CANCEL))) {
                    result.add(CMD_CANCEL);
                }
                if (perms.contains(getPermission(CMD_EXIT))) {
                    result.add(CMD_EXIT);
                }
                if (perms.contains(getPermission(CMD_END))) {
                    result.add(CMD_END);
                }
                if (perms.contains(getPermission(CMD_RELOAD))) {
                    result.add(CMD_RELOAD);
                }
                if (perms.contains(getPermission(CMD_HELP))) {
                    result.add(CMD_HELP);
                }
            } else if (args.size() == 2 && args.get(0).equals(CMD_START) && perms.contains(getPermission(CMD_START))) {
                result.addAll(getAvailableRaids());
            }
        }

        return result;
    }

    public static String getPermission(String command) {
        return "raids." + command;
    }

    public List<String> getAvailableRaids() {
        return getRaidsConfig().getRaids().stream().map(Raid::getName).collect(Collectors.toList());
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

                try {
                    removeWorld(w);
                } catch (IOException ioExc) {
                    getLogger().severe("Unable to remove " + w.getName());
                    getLogger().throwing(RaidsManager.class.getName(), "cleanRaids", ioExc);
                }
            }
        }
    }

    public void deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            assert files != null;
            for (File value : files) {
                deleteFile(value);
            }
        }

        Files.delete(Paths.get(file.toURI()));
    }

    public void removeWorld(World world) throws IOException {
        if (EntityFactory.getInstance().getServer().unloadWorld(world.getName())) {
            getLogger().info("Removing raid " + world.getName());
            deleteFile(world.getWorldFolder());
            getLogger().info(world.getName() + " removed.");
        }
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

    public void startRaid(UUID partyId, String newWorld, Raid raid) {
        queueParty(partyId, newWorld);

        Party party = PartyFactory.getInstance().getParty(partyId);

        if (raid.getJoinIn() > 0) {
            party.broadcastMessage("Party is queued for a raid.");
            party.broadcastMessage("Starting in " + raid.getJoinIn() + " seconds.");
            party.broadcastMessage("Use /raids cancel to abort.");
        }

        EntityFactory.getInstance().getServer().delayRunnable(new StartRaidRunnable(partyId), (long) raid.getJoinIn() * 20);
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

                if (player != null && perms.contains(getPermission(args.get(0)))) {
                    switch (args.get(0)) {
                        case CMD_RELOAD:
                            reload();
                            receiver.sendMessage("Config reloaded. Found " + getAvailableRaids().size() + " raids.");
                            break;
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
                                    Raid raid = getRaid(args.get(1));
                                    if (raid != null) {
                                        receiver.sendMessage("Creating raid dungeon");

                                        String newWorld = "raid_" + raid.getName() + "_" + System.currentTimeMillis();

                                        try {
                                            cloneWorld(raid.getDungeonName(), newWorld);
                                            startRaid(partyId, newWorld, raid);
                                        } catch (IOException e) {
                                            receiver.sendMessage("Error creating raid");
                                            getLogger().throwing(RaidsPlugin.class.getName(), "onCommand", e);
                                        }

                                    } else {
                                        receiver.sendMessage("Could not find raid raid");
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

    private Raid getRaid(String name) {
        return getRaidsConfig().getRaids().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<World> getActiveRaids() {
        return getServer().getWorlds().stream()
                .filter(w -> w.getName().startsWith("raid_"))
                .collect(Collectors.toList());
    }

    class RaidScrubber implements Runnable {
        public void run() {
            if (running) {
                cleanRaids();
                startScrubber();
            }
        }
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
