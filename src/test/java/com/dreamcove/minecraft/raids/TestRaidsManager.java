package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.PartyFactory;
import com.dreamcove.minecraft.raids.api.World;
import com.dreamcove.minecraft.raids.api.WorldLocation;
import com.dreamcove.minecraft.raids.config.Point;
import com.dreamcove.minecraft.raids.utils.FileUtilities;
import org.bukkit.WorldCreator;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestRaidsManager {
    private final File dataDirectory = new File(new File(new File("target"), "test-data"), "plugin");
    private RaidsManager manager;
    private List<String> allPerms;
    private TestEntityFactory.TestPlayer player1;
    private TestEntityFactory.TestPlayer player2;
    private TestEntityFactory.TestPlayer player3;
    private TestPartyFactory.TestParty party1;
    private TestEntityFactory.TestWorld emptyWorld;

    @BeforeAll
    public static void load() {
        EntityFactory.setInstance(new TestEntityFactory());
        PartyFactory.setInstance(new TestPartyFactory());
    }

    @BeforeEach
    public void testSetup() throws IOException {
        FileUtilities.deleteFile(new File(new File("target"), "test-data"));

        allPerms = RaidsManager.ALL_COMMANDS.stream().map(RaidsManager::getPermission).collect(Collectors.toList());

        manager = new RaidsManager(dataDirectory);

        emptyWorld = (TestEntityFactory.TestWorld) EntityFactory.getInstance().getServer().createWorld(new WorldCreator("empty_world"));

        player1 = getNewPlayer();
        player2 = getNewPlayer();
        player3 = getNewPlayer();

        party1 = new TestPartyFactory.TestParty(UUID.randomUUID().toString());

        TestEntityFactory.TestServer server = (TestEntityFactory.TestServer) EntityFactory.getInstance().getServer();
        server.addPlayer(player1);
        server.addPlayer(player2);
        server.addPlayer(player3);

        party1.addMember(player1.getUniqueId());
        party1.addMember(player2.getUniqueId());

        ((TestPartyFactory) PartyFactory.getInstance()).addParty(party1);

    }

    private TestEntityFactory.TestPlayer getNewPlayer() {
        TestEntityFactory.TestPlayer player = new TestEntityFactory.TestPlayer(UUID.randomUUID().toString());
        player.teleport(emptyWorld.getSpawnLocation());

        return player;
    }

    @AfterEach
    public void testShutdown() {
        manager.shutdown();
    }

    @Test
    public void testHelp() {
        Assertions.assertEquals(0, manager.getHelp(new ArrayList<>()).size());

        Assertions.assertEquals(6, manager.getHelp(allPerms).size());
    }

    @Test
    public void testLastLocation() {

        LocationManager locManager = new LocationManager(new File(dataDirectory, "test-locations.yml"));

        UUID uuid = UUID.randomUUID();

        WorldLocation newLoc = new WorldLocation(emptyWorld, new Point(5, 5, 5));

        Assertions.assertNull(locManager.get(uuid));

        locManager.store(uuid, newLoc);

        // Ensure we're keeping the map
        Assertions.assertEquals(newLoc, locManager.get(uuid));

        // New instance to restore locations
        locManager = new LocationManager(new File(dataDirectory, "test-locations.yml"));

        Assertions.assertEquals(newLoc, locManager.get(uuid));

        // Remove should return value and remove it
        Assertions.assertEquals(newLoc, locManager.remove(uuid));

        Assertions.assertNull(locManager.get(uuid));
    }

    @Test
    public void testAvailableRaids() {
        List<String> names = manager.getAvailableRaids();

        Assertions.assertEquals(1, names.size());
        Assertions.assertTrue(names.contains("example"));
    }

    @Test
    public void testTabComplete() {
        List<String> result = manager.getTabComplete("raids", allPerms, Collections.singletonList(""));

        for (String cmd : RaidsManager.ALL_COMMANDS) {
            Assertions.assertTrue(result.contains(cmd));
        }

        result = manager.getTabComplete("raids", new ArrayList<>(), Collections.singletonList(""));
        Assertions.assertEquals(0, result.size());

        result = manager.getTabComplete("raids", allPerms, Arrays.asList("start", ""));

        for (String raid : manager.getAvailableRaids()) {
            Assertions.assertTrue(result.contains(raid));
        }

        final List<String> startResult = manager.getTabComplete("raids", allPerms, Arrays.asList("package", ""));

        EntityFactory.getInstance().getServer().getWorlds().stream()
                .map(World::getName)
                .filter(n -> !n.startsWith(manager.getRaidsConfig().getRaidWorldPrefix()))
                .forEach(n -> Assertions.assertTrue(startResult.contains(n)));

        final List<String> packageResult = manager.getTabComplete("raids", allPerms, Arrays.asList("package", ""));

        EntityFactory.getInstance().getServer().getWorlds().stream()
                .map(World::getName)
                .filter(n -> !n.startsWith(manager.getRaidsConfig().getRaidWorldPrefix()))
                .forEach(n -> Assertions.assertTrue(packageResult.contains(n)));
    }

    @Test
    public void testCommandStart() {
        resetAllRaids();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "example"), allPerms));

        Assertions.assertNotNull(manager.getRaidByParty(party1.getId()));

        Assertions.assertTrue(manager.cancelRaid(party1.getId()));

        Assertions.assertNull(manager.getRaidByParty(party1.getId()));
    }

    private void resetAllRaids() {
        RaidManagedWorld world;

        while ((world = manager.getRaidByParty(party1.getId())) != null) {
            world.setState(RaidManagedWorld.STATE_CANCELED);
            world.getWorld().getPlayers().clear();
        }
    }

    @Test
    public void testCommandCancel() {
        resetAllRaids();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "example"), allPerms));

        Assertions.assertNotNull(manager.getRaidByParty(party1.getId()));

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("cancel"), allPerms));

        Assertions.assertNull(manager.getRaidByParty(party1.getId()));
    }

    @Test
    public void testCommandStartWithoutParty() {
        List<World> worlds = EntityFactory.getInstance().getServer().getWorlds();
        Assertions.assertTrue(manager.processCommand(player3, "raids", Arrays.asList("start", "example"), allPerms));

        Assertions.assertEquals(worlds.size(), EntityFactory.getInstance().getServer().getWorlds().size());
    }

    @Test
    public void testCommandEnd() {
        WorldLocation loc1 = player1.getLocation();
        WorldLocation loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "example"), allPerms));

        Assertions.assertNotNull(manager.getRaidByParty(party1.getId()));

        try {
            Thread.sleep(6 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assertions.assertNotEquals(loc1, player1.getLocation());
        Assertions.assertNotEquals(loc2, player2.getLocation());

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("end"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertEquals(loc2, player2.getLocation());
    }

    @Test
    public void testCommandReload() {
        manager.getRaidsConfig().getRaids().clear();

        Assertions.assertEquals(0, manager.getAvailableRaids().size());

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("reload"), allPerms));

        Assertions.assertEquals(1, manager.getAvailableRaids().size());
    }

    @Test
    public void testCommandHelp() {
        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("help"), allPerms));
    }

    @Test
    public void testCommandExit() {
        WorldLocation loc1 = player1.getLocation();
        WorldLocation loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "example"), allPerms));

        Assertions.assertNotNull(manager.getRaidByParty(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) manager.getRaidByParty(party1.getId()).getWorld();

        try {
            Thread.sleep(6 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assertions.assertNotEquals(loc1, player1.getLocation());
        Assertions.assertNotEquals(loc2, player2.getLocation());

        // Minor nudge for testing purposes
        player1.setWorld(world);
        player2.setWorld(world);
        world.addPlayer(player1);
        world.addPlayer(player2);

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("exit"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertNotEquals(loc2, player2.getLocation());

        Assertions.assertTrue(manager.processCommand(player2, "raids", Collections.singletonList("exit"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertEquals(loc2, player2.getLocation());
    }

    @Test
    public void testCleanCycle() {
        resetAllRaids();

        WorldLocation loc1 = player1.getLocation();
        WorldLocation loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "example"), allPerms));

        Assertions.assertNotNull(manager.getRaidByParty(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) manager.getRaidByParty(party1.getId()).getWorld();

        try {
            Thread.sleep(6 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assertions.assertNotEquals(loc1, player1.getLocation());
        Assertions.assertNotEquals(loc2, player2.getLocation());

        // Minor nudge for testing purposes
        player1.setWorld(world);
        player2.setWorld(world);
        world.addPlayer(player1);
        world.addPlayer(player2);

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("exit"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertNotEquals(loc2, player2.getLocation());

        Assertions.assertTrue(manager.processCommand(player2, "raids", Collections.singletonList("exit"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertEquals(loc2, player2.getLocation());

        Assertions.assertNotNull(manager.getManagedWorld(world.getName()));

        world.clearPlayers();
        // Wait 20 seconds for cleanup cycle

        try {
            Thread.sleep(20000);
        } catch (Throwable ignored) {
        }

        Assertions.assertNull(manager.getManagedWorld(world.getName()));
    }

    @Test
    public void testCommandPackage() throws IOException {
        // Create a temporary world
        File worldDir = new File(EntityFactory.getInstance().getServer().getWorldContainer(), "temp");
        File childDir = new File(worldDir, "child");
        childDir.mkdirs();
        File testFile = new File(childDir, "test.txt");

        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(UUID.randomUUID().toString().getBytes());
        }

        WorldCreator creator = new WorldCreator("temp");
        EntityFactory.getInstance().getServer().createWorld(creator);

        Assertions.assertFalse(manager.getAvailableDungeons().contains("new-dungeon"));

        Assertions.assertTrue(manager.processCommand(player2, "raids", Arrays.asList("package", "temp", "new-dungeon"), allPerms));

        Assertions.assertTrue(manager.getAvailableDungeons().contains("new-dungeon"));
    }

    @Test
    public void testCommandPackageFailAlreadyExists() throws IOException, InterruptedException {
        // Create a temporary world
        File worldDir = new File(EntityFactory.getInstance().getServer().getWorldContainer(), "temp");
        File childDir = new File(worldDir, "child");
        childDir.mkdirs();
        File testFile = new File(childDir, "test.txt");

        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(UUID.randomUUID().toString().getBytes());
        }

        WorldCreator creator = new WorldCreator("temp");
        EntityFactory.getInstance().getServer().createWorld(creator);

        Assertions.assertFalse(manager.getAvailableDungeons().contains("new-dungeon-1"));

        Assertions.assertTrue(manager.processCommand(player2, "raids", Arrays.asList("package", "temp", "new-dungeon-1"), allPerms));

        Assertions.assertTrue(manager.getAvailableDungeons().contains("new-dungeon-1"));

        long origDungeonTimestamp = new File(new File(manager.getDataDirectory(), "dungeons"), "new-dungeon-1.zip").lastModified();

        Thread.sleep(2000);

        Assertions.assertTrue(manager.processCommand(player2, "raids", Arrays.asList("package", "temp", "new-dungeon-1"), allPerms));

        long newDungeonTimestamp = new File(new File(manager.getDataDirectory(), "dungeons"), "new-dungeon-1.zip").lastModified();

        Assertions.assertEquals(origDungeonTimestamp, newDungeonTimestamp);
    }


    @Test
    public void testCommandPackageUpdate() throws IOException, InterruptedException {
        // Create a temporary world
        File worldDir = new File(EntityFactory.getInstance().getServer().getWorldContainer(), "temp");
        File childDir = new File(worldDir, "child");
        childDir.mkdirs();
        File testFile = new File(childDir, "test.txt");

        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(UUID.randomUUID().toString().getBytes());
        }

        WorldCreator creator = new WorldCreator("temp");
        EntityFactory.getInstance().getServer().createWorld(creator);

        Assertions.assertFalse(manager.getAvailableDungeons().contains("new-dungeon-2"));

        Assertions.assertTrue(manager.processCommand(player2, "raids", Arrays.asList("package", "temp", "new-dungeon-2"), allPerms));

        Assertions.assertTrue(manager.getAvailableDungeons().contains("new-dungeon-2"));

        long origDungeonTimestamp = new File(new File(manager.getDataDirectory(), "dungeons"), "new-dungeon-2.zip").lastModified();

        Thread.sleep(2000);

        Assertions.assertTrue(manager.processCommand(player2, "raids", Arrays.asList("package", "temp", "new-dungeon-2", "-f"), allPerms));

        long newDungeonTimestamp = new File(new File(manager.getDataDirectory(), "dungeons"), "new-dungeon-2.zip").lastModified();

        Assertions.assertNotEquals(origDungeonTimestamp, newDungeonTimestamp);
    }

    @Test
    public void testCommandEditAndSaveAndExit() throws InterruptedException {
        resetAllRaids();

        player1.setWorld(EntityFactory.getInstance().getServer().getWorld("empty_world"));

        Assertions.assertEquals(0, manager.getDungeonManagedWorlds().size());

        File file = new File(new File(manager.getDataDirectory(), "dungeons"), "arena.zip");
        long time = file.lastModified();

        Thread.sleep(2000);

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("edit", "arena"), allPerms));

        Assertions.assertEquals(1, manager.getDungeonManagedWorlds().size());

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("save"), allPerms));

        Assertions.assertNotEquals(time, file.lastModified());

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("exit"), allPerms));

        Thread.sleep(10000);

        Assertions.assertEquals(0, manager.getDungeonManagedWorlds().size());
    }


}
