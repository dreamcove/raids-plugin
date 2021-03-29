package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.PartyFactory;
import com.dreamcove.minecraft.raids.api.World;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestRaidsManager {
    private static RaidsManager manager;
    private static List<String> allPerms;

    private static TestEntityFactory.TestPlayer player1;
    private static TestEntityFactory.TestPlayer player2;
    private static TestEntityFactory.TestPlayer player3;

    private static TestPartyFactory.TestParty party1;

    @BeforeAll
    public static void load() {
        allPerms = RaidsManager.ALL_COMMANDS.stream().map(RaidsManager::getPermission).collect(Collectors.toList());

        EntityFactory.setInstance(new TestEntityFactory());
        PartyFactory.setInstance(new TestPartyFactory());

        manager = new RaidsManager(TestRaidsManager.class.getClassLoader().getResource("test-config.yml"), null);

        player1 = new TestEntityFactory.TestPlayer(UUID.randomUUID().toString());
        player2 = new TestEntityFactory.TestPlayer(UUID.randomUUID().toString());
        player3 = new TestEntityFactory.TestPlayer(UUID.randomUUID().toString());

        party1 = new TestPartyFactory.TestParty(UUID.randomUUID().toString());

        TestEntityFactory.TestServer server = (TestEntityFactory.TestServer) EntityFactory.getInstance().getServer();
        server.addPlayer(player1);
        server.addPlayer(player2);
        server.addPlayer(player3);

        party1.addMember(player1.getUniqueId());
        party1.addMember(player2.getUniqueId());

        ((TestPartyFactory) PartyFactory.getInstance()).addParty(party1);


        File worldDir = new File(server.getWorldContainer(), "template_arena");
        File childDir = new File(worldDir, "child");
        childDir.mkdirs();
        try {
            File tempFile = new File(childDir, "tempfile.txt");
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(UUID.randomUUID().toString().getBytes());
            fos.close();

            new File(worldDir, "uid.dat").createNewFile();
            server.createWorld(new WorldCreator("template_arena"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void unload() {
        // Test shutdown
        Assertions.assertNotEquals(0, manager.getActiveRaids().size());

        manager.shutdown();

        Assertions.assertEquals(0, manager.getActiveRaids().size());

        Assertions.assertEquals(2, EntityFactory.getInstance().getServer().getWorldContainer().listFiles().length);
    }

    @Test
    public void testHelp() {
        Assertions.assertEquals(0, manager.getHelp(new ArrayList<>()).size());

        Assertions.assertEquals(5, manager.getHelp(allPerms).size());
    }

    @Test
    public void testQueueParty() {
        UUID uuid = UUID.randomUUID();

        Assertions.assertFalse(manager.isPartyQueued(uuid));

        manager.queueParty(uuid, "test");

        Assertions.assertTrue(manager.isPartyQueued(uuid));
        Assertions.assertEquals("test", manager.getQueuedWorld(uuid));

        manager.dequeueParty(uuid);

        Assertions.assertFalse(manager.isPartyQueued(uuid));
    }

    @Test
    public void testLastLocation() {
        Location newLoc = new Location(null, 23, 44, 33);
        TestEntityFactory.TestPlayer newPlayer = new TestEntityFactory.TestPlayer("abc");

        Assertions.assertNull(manager.getLastLocation(newPlayer));

        Location oldLoc = newPlayer.getLocation();

        manager.storeLastLocation(newPlayer);

        Assertions.assertEquals(oldLoc, manager.getLastLocation(newPlayer));

        newPlayer.teleport(newLoc);

        Assertions.assertEquals(newLoc, newPlayer.getLocation());

        manager.returnLastLocation(newPlayer);

        Assertions.assertNull(manager.getLastLocation(newPlayer));
        Assertions.assertEquals(oldLoc, newPlayer.getLocation());
    }

    @Test
    public void testCloneWorld() {
        try {
            manager.cloneWorld("template_arena", "new-world");

            World newWorld = EntityFactory.getInstance().getServer().getWorld("new-world");
            World oldWorld = EntityFactory.getInstance().getServer().getWorld("template_arena");

            Assertions.assertNotNull(newWorld);
            Assertions.assertNotNull(oldWorld);

            Assertions.assertTrue(new File(oldWorld.getWorldFolder(), "uid.dat").exists());
            Assertions.assertFalse(new File(newWorld.getWorldFolder(), "uid.dat").exists());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testRemoveWorld() {
        try {
            manager.cloneWorld("template_arena", "new-world-1");

            World newWorld = EntityFactory.getInstance().getServer().getWorld("new-world-1");
            World oldWorld = EntityFactory.getInstance().getServer().getWorld("template_arena");

            Assertions.assertNotNull(newWorld);
            Assertions.assertNotNull(oldWorld);

            manager.removeWorld(newWorld);

            Assertions.assertNull(EntityFactory.getInstance().getServer().getWorld(newWorld.getName()));
            Assertions.assertFalse(newWorld.getWorldFolder().exists());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testAvailableRaids() {
        List<String> names = manager.getAvailableRaids();

        Assertions.assertEquals(2, names.size());
        Assertions.assertTrue(names.contains("arena_1"));
        Assertions.assertTrue(names.contains("arena_2"));
    }

    @Test
    public void testCleanRaids() {
        try {
            manager.cloneWorld("template_arena", "raid_arena_123456");

            Assertions.assertNotNull(EntityFactory.getInstance().getServer().getWorld("raid_arena_123456"));

            manager.cleanRaids();

            Assertions.assertNull(EntityFactory.getInstance().getServer().getWorld("raid_arena_123456"));
        } catch (Throwable t) {
            Assertions.fail(t);
        }
    }

    @Test
    public void testTabComplete() {
        List<String> result = manager.getTabComplete("raids", allPerms, Collections.singletonList(""));

        for (String cmd : RaidsManager.ALL_COMMANDS) {
            Assertions.assertTrue(result.contains(cmd));
        }

        result = manager.getTabComplete("raids", new ArrayList<>(), Collections.singletonList(""));
        Assertions.assertEquals(1, result.size());

        result = manager.getTabComplete("raids", allPerms, Arrays.asList("start", ""));

        for (String raid : manager.getAvailableRaids()) {
            Assertions.assertTrue(result.contains(raid));
        }
    }

    @Test
    public void testCommandStart() {
        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena_1"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        manager.cancelRaid(party1.getId());

        Assertions.assertFalse(manager.isPartyQueued(party1.getId()));
    }

    @Test
    public void testCommandCancel() {
        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena_1"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("cancel"), allPerms));

        Assertions.assertFalse(manager.isPartyQueued(party1.getId()));
    }

    @Test
    public void testCommandStartWithoutParty() {
        List<World> worlds = EntityFactory.getInstance().getServer().getWorlds();
        Assertions.assertTrue(manager.processCommand(player3, "raids", Arrays.asList("start", "arena_1"), allPerms));

        Assertions.assertEquals(worlds.size(), EntityFactory.getInstance().getServer().getWorlds().size());
    }

    @Test
    public void testCommandEnd() {
        Location loc1 = player1.getLocation();
        Location loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena_1"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) EntityFactory.getInstance().getServer().getWorld(manager.getQueuedWorld(party1.getId()));

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

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("end"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertEquals(loc2, player2.getLocation());
    }

    @Test
    public void testCommandReload() {
        manager.getRaidsConfig().getRaids().clear();

        Assertions.assertEquals(0, manager.getAvailableRaids().size());

        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("reload"), allPerms));

        Assertions.assertEquals(2, manager.getAvailableRaids().size());
    }

    @Test
    public void testCommandHelp() {
        Assertions.assertTrue(manager.processCommand(player1, "raids", Collections.singletonList("help"), allPerms));
    }

    @Test
    public void testCommandExit() {
        Location loc1 = player1.getLocation();
        Location loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena_1"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) EntityFactory.getInstance().getServer().getWorld(manager.getQueuedWorld(party1.getId()));

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
        Location loc1 = player1.getLocation();
        Location loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena_1"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) EntityFactory.getInstance().getServer().getWorld(manager.getQueuedWorld(party1.getId()));

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

        Assertions.assertTrue(manager.getActiveRaids().contains(world));

        world.clearPlayers();
        // Wait 20 seconds for cleanup cycle

        try {
            Thread.sleep(20000);
        } catch (Throwable t) {
        }

        Assertions.assertFalse(manager.getActiveRaids().contains(world));
    }
}
