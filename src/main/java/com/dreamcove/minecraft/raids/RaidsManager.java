package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import com.dreamcove.minecraft.raids.config.Raid;
import com.dreamcove.minecraft.raids.config.RaidsConfig;
import org.bukkit.Difficulty;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
    public static final String CMD_PACKAGE = "package";
    public static final List<String> ALL_COMMANDS = Arrays.asList(
            CMD_RELOAD,
            CMD_START,
            CMD_CANCEL,
            CMD_END,
            CMD_EXIT,
            CMD_PACKAGE,
            CMD_HELP
    );
    private final Map<UUID, String> queuedParties = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, WorldLocation> lastLocation = Collections.synchronizedMap(new HashMap<>());
    private final File dataDirectory;
    private Logger logger;
    private RaidsConfig raidsConfig;
    private boolean running;

    // Constructors
    public RaidsManager(File dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        initialize();
    }

    public static String getPermission(String command) {
        return "raids." + command;
    }

    private void initialize() {
        running = true;
        try {
            loadDefaults();
        } catch (IOException | InvalidConfigurationException exc) {
            getLogger().warning("Error loading defaults: " + exc);
        }

        startScrubber();
    }

    private void startScrubber() {
        int cycle = Math.max(getRaidsConfig().getCleanCycle(), 5);
        EntityFactory.getInstance().getServer().delayRunnable(() -> {
            if (running) {
                cleanRaids();
                startScrubber();
            }
        }, cycle * 20L);
    }

    public synchronized RaidsConfig getRaidsConfig() {
        if (raidsConfig == null) {
            InputStream is = null;
            BufferedReader br = null;

            File file = new File(getDataDirectory(), "config.yml");

            try {
                is = new FileInputStream(file);
                br = new BufferedReader(new InputStreamReader(is));

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    result.append(line).append("\n");
                }

                YamlConfiguration yamlConfig = new YamlConfiguration();
                yamlConfig.loadFromString(result.toString());

                raidsConfig = RaidsConfig.from(yamlConfig);
            } catch (Exception ioExc) {
                getLogger().severe("Error loading configuration");
                getLogger().throwing("RaidsManager", "getRaidsConfig", ioExc);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ioExc) {
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioExc) {
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

    private void packageFile(ZipOutputStream zos, File file, String entryName) throws IOException {
        if (file.isDirectory()) {
            zos.putNextEntry(new ZipEntry(entryName + "/"));

            for (File f : Objects.requireNonNull(file.listFiles())) {
                packageFile(zos, f, entryName + "/" + f.getName());
            }
        } else {
            zos.putNextEntry(new ZipEntry(entryName));

            FileInputStream fis = null;

            try {
                fis = new FileInputStream(file);

                byte[] buffer = new byte[1024];
                int read;

                while ((read = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, read);
                }

                zos.closeEntry();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void packageWorld(String worldName, String dungeonName, boolean forceUpdate) throws Exception {
        World w = EntityFactory.getInstance().getServer().getWorld(worldName);

        if (w == null) {
            throw new Exception("World " + worldName + " does not exist");
        }

        if (getAvailableDungeons().contains(dungeonName) && !forceUpdate) {
            throw new Exception("Dungeon " + dungeonName + " already exists");
        }

        ZipOutputStream zos = null;

        try {
            File dungeonFile = new File(getDungeonDirectory(), dungeonName + ".zip");
            zos = new ZipOutputStream(new FileOutputStream(dungeonFile));

            for (File f : Objects.requireNonNull(w.getWorldFolder().listFiles())) {
                packageFile(zos, f, f.getName());
            }
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    public void shutdown() {
        running = false;

        getActiveRaids().forEach(w -> {
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

        if (perms.contains(getPermission(CMD_PACKAGE))) {
            result.add("/raids package <world> <dungeon> [-f] - Package a world as dungeon level");
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
                if (perms.contains(getPermission(CMD_PACKAGE))) {
                    result.add(CMD_PACKAGE);
                }
            } else if (args.size() == 2 && args.get(0).equals(CMD_START) && perms.contains(getPermission(CMD_START))) {
                result.addAll(getAvailableRaids());
            } else if (args.size() == 2 && args.get(0).equals(CMD_PACKAGE) && perms.contains(getPermission(CMD_PACKAGE))) {
                result.addAll(EntityFactory.getInstance().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(n -> !n.startsWith(getRaidsConfig().getRaidWorldPrefix()))
                        .collect(Collectors.toList())
                );
            }
        }

        return result;
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

    public WorldLocation getLastLocation(Player player) {
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
            if (!queuedParties.containsValue(w.getName()) && w.getName().startsWith(getRaidsConfig().getRaidWorldPrefix()) && w.getPlayers().isEmpty()) {
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

    public boolean cancelRaid(UUID partyId) {
        if (isPartyQueued(partyId)) {
            dequeueParty(partyId);

            PartyFactory.getInstance().getParty(partyId).broadcastMessage("Raid canceled");

            return true;
        }

        return false;
    }

    public void startRaid(UUID partyId, World newWorld, Raid raid) {
        queueParty(partyId, newWorld.getName());

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
                            break;
                        case CMD_PACKAGE:
                            if (args.size() >= 3) {
                                String worldName = args.get(1);
                                String dungeonName = args.get(2);
                                boolean forceUpdate = (args.size() > 3 && args.get(3).equals("-f"));

                                try {
                                    packageWorld(worldName, dungeonName, forceUpdate);
                                    receiver.sendMessage("World " + worldName + " has been packaged as dungeon " + dungeonName);
                                } catch (Exception e) {
                                    receiver.sendMessage(e.getMessage());
                                }
                            } else {
                                receiver.sendMessage("/raids start requires 2 arguments");
                            }
                            break;
                        case CMD_START:
                            if (args.size() == 2) {
                                // Find Party
                                final UUID playerId = player.getUniqueId();
                                final UUID partyId = PartyFactory.getInstance().getPartyForPlayer(playerId);
                                if (partyId == null) {
                                    receiver.sendMessage("Player must belong to party");
                                } else {
                                    Party party = PartyFactory.getInstance().getParty(partyId);
                                    Raid raid = getRaid(args.get(1));

                                    if (raid != null) {
                                        int minLevel = party.getMembers().stream()
                                                .map(u -> EntityFactory.getInstance().getServer().getPlayer(u))
                                                .map(Player::getLevel)
                                                .reduce(Integer.MAX_VALUE, (acc, n) -> Math.min(acc, n));

                                        if (party.getMembers().size() < raid.getJoinCriteria().getMinimumPartySize()) {
                                            receiver.sendMessage("Your party must have " + raid.getJoinCriteria().getMinimumPartySize() + " members to start raid");
                                            return true;
                                        }

                                        if (minLevel < raid.getJoinCriteria().getMinimumLevel()) {
                                            receiver.sendMessage("All members of your party must have at least a level of " + raid.getJoinCriteria().getMinimumLevel());
                                        }

                                        receiver.sendMessage("Creating raid dungeon");

                                        try {
                                            World w = setupRaidWorld(raid);
                                            startRaid(partyId, w, raid);
                                        } catch (IOException e) {
                                            receiver.sendMessage("Error creating raid: " + e);
                                            getLogger().throwing(RaidsPlugin.class.getName(), "onCommand", e);
                                        }

                                    } else {
                                        receiver.sendMessage("Could not find raid raid");
                                    }
                                }
                            } else {
                                receiver.sendMessage("/raids start requires 1 arguments");
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
                .filter(w -> w.getName().startsWith(getRaidsConfig().getRaidWorldPrefix()))
                .collect(Collectors.toList());
    }

    private World generateDungeon(String dungeonName) throws IOException {
        String filename = dungeonName + ".zip";

        File dungeonFile = new File(getDungeonDirectory(), filename);

        if (!dungeonFile.exists()) {
            // See if there is a default dungeon by that name for loading
            URL url = Thread.currentThread().getContextClassLoader().getResource("dungeons/" + filename);

            if (url == null) {
                throw new IOException("Dungeon " + dungeonName + " not found");
            }

            InputStream is = null;
            OutputStream os = null;

            try {
                is = url.openStream();
                os = new FileOutputStream(dungeonFile);

                byte[] buffer = new byte[1024];
                int read;

                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                }
            }
        }

        String worldName = getRaidsConfig().getRaidWorldPrefix() + "_" + UUID.randomUUID().toString();
        File worldDir = new File(getServer().getWorldContainer(), worldName);

        ZipInputStream zis = null;

        try {
            zis = new ZipInputStream(new FileInputStream(dungeonFile));

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().endsWith("uid.dat") && !entry.getName().endsWith("session.lock")) {
                    File file = new File(worldDir, entry.getName());
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        file.getParentFile().mkdirs();

                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int read;

                            while ((read = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, read);
                            }
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                }
            }
        }

        WorldCreator creator = new WorldCreator(worldName);
        return EntityFactory.getInstance().getServer().createWorld(creator);
    }

    private World setupRaidWorld(Raid raid) throws IOException {
        World world = generateDungeon(raid.getDungeonName());

        // Set Spawn Location
        world.setSpawnLocation(
                new WorldLocation(
                        world,
                        raid.getSpawnLocation()
                ));

        // clear all existing mobs
        if (raid.getOnStartup().isClearMobs()) {
            world.removeAllEntities();
        }

        world.setDifficulty(Difficulty.valueOf(raid.getDifficulty().toUpperCase()));

        raid.getOnStartup().getMobs().stream()
                .forEach(m -> {
                    getLogger().info("Spawning " + m.getType());
                    world.spawnEntity(
                            EntityType.valueOf(EntityType.class, m.getType().toUpperCase()),
                            m.getLocation().getX(), m.getLocation().getY(), m.getLocation().getZ());
                });

        raid.getOnStartup().getCommands().stream()
                .map(c -> c.replaceAll("@w", world.getName()))
                .forEach(c -> {
                    getLogger().info("Executing start-up command: " + c);
                    getServer().dispatchCommand(c);
                });

        return world;
    }

    public List<String> getAvailableDungeons() {
        return Arrays.stream(Objects.requireNonNull(getDungeonDirectory().listFiles()))
                .map(File::getName)
                .filter(n -> n.endsWith(".zip"))
                .map(n -> n.substring(0, n.lastIndexOf(".")))
                .collect(Collectors.toList());
    }

    private File getDungeonDirectory() {
        File result = new File(getDataDirectory(), "dungeons");
        if (!result.exists()) {
            result.mkdirs();
        }

        return result;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    private void downloadResource(URL resource, File file) throws IOException {
        file.getParentFile().mkdirs();

        InputStream is = null;
        FileOutputStream fos = null;

        try {
            is = resource.openStream();
            fos = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int read;

            while ((read = is.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void loadDefaults() throws IOException, InvalidConfigurationException {
        File configFile = new File(getDataDirectory(), "config.yml");

        if (!configFile.exists()) {
            try {
                downloadResource(RaidsManager.class.getClassLoader().getResource("config.yml"), configFile);
                downloadResource(
                        RaidsManager.class.getClassLoader().getResource("dungeons/arena.zip"), new File(getDungeonDirectory(), "arena.zip"));
            } catch (IOException e) {
                getLogger().warning("Unable to load defaults: " + e);
            }

            YamlConfiguration yamlConfig = new YamlConfiguration();
            yamlConfig.load(configFile);

            raidsConfig = RaidsConfig.from(yamlConfig);
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
