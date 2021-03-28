package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.PartyFactory;
import com.dreamcove.minecraft.raids.api.World;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestRaidsManager {
    private static RaidsManager manager;
    private static List<String> allCommands;
    private static List<String> allPerms;

    private static TestEntityFactory.TestPlayer player1;
    private static TestEntityFactory.TestPlayer player2;
    private static TestEntityFactory.TestPlayer player3;

    private static TestPartyFactory.TestParty party1;

    @BeforeAll
    public static void load() {
        allCommands = Arrays.asList(
                RaidsManager.CMD_RELOAD,
                RaidsManager.CMD_START,
                RaidsManager.CMD_CANCEL,
                RaidsManager.CMD_END,
                RaidsManager.CMD_EXIT
        );
        allPerms = allCommands.stream().map(c -> "raids." + c).collect(Collectors.toList());

        FileConfiguration file = new FileConfiguration() {
            @Override
            public String saveToString() {
                return "";
            }

            @Override
            public void loadFromString(String s) throws InvalidConfigurationException {
            }

            @Override
            protected String buildHeader() {
                return "";
            }
        };

        manager = new RaidsManager(null);
        EntityFactory.setInstance(new TestEntityFactory());
        PartyFactory.setInstance(new TestPartyFactory());

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
        worldDir.mkdirs();
        try {
            new File(worldDir, "tempfile.txt").createNewFile();
            new File(worldDir, "uid.dat").createNewFile();
            server.createWorld(new WorldCreator("template_arena"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @AfterAll
    public static void unload() {
    }

    @Test
    public void testHelp() {
        Assertions.assertEquals(0, manager.getHelp(Collections.EMPTY_LIST).size());

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

        Assertions.assertEquals(null, manager.getLastLocation(newPlayer));

        Location oldLoc = newPlayer.getLocation();

        manager.storeLastLocation(newPlayer);

        Assertions.assertEquals(oldLoc, manager.getLastLocation(newPlayer));

        newPlayer.teleport(newLoc);

        Assertions.assertEquals(newLoc, newPlayer.getLocation());

        manager.returnLastLocation(newPlayer);

        Assertions.assertEquals(null, manager.getLastLocation(newPlayer));
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

        Assertions.assertEquals(1, names.size());
        Assertions.assertTrue(names.contains("arena"));
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
        List<String> allPerms = allCommands.stream().map(p -> "raids." + p).collect(Collectors.toList());

        List<String> result = manager.getTabComplete("raids", allPerms, Arrays.asList(""));

        for (String cmd : allCommands) {
            Assertions.assertTrue(result.contains(cmd));
        }

        result = manager.getTabComplete("raids", Collections.EMPTY_LIST, Arrays.asList(""));
        Assertions.assertEquals(1, result.size());

        result = manager.getTabComplete("raids", allPerms, Arrays.asList("start", ""));

        for (String raid : manager.getAvailableRaids()) {
            Assertions.assertTrue(result.contains(raid));
        }
    }

    @Test
    public void testCommandStart() {
        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        manager.cancelRaid(party1.getId());

        Assertions.assertFalse(manager.isPartyQueued(party1.getId()));
    }

    @Test
    public void testCommandCancel() {
        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("cancel"), allPerms));

        Assertions.assertFalse(manager.isPartyQueued(party1.getId()));
    }

    @Test
    public void testCommandStartWithoutParty() {
        List<World> worlds = EntityFactory.getInstance().getServer().getWorlds();
        Assertions.assertTrue(manager.processCommand(player3, "raids", Arrays.asList("start", "arena"), allPerms));

        Assertions.assertEquals(worlds.size(), EntityFactory.getInstance().getServer().getWorlds().size());
    }

    @Test
    public void testCommandEnd() {
        Location loc1 = player1.getLocation();
        Location loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) EntityFactory.getInstance().getServer().getWorld(manager.getQueuedWorld(party1.getId()));

        try {
            Thread.sleep(16 * 1000);
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

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("end"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertEquals(loc2, player2.getLocation());
    }

    @Test
    public void testCommandExit() {
        Location loc1 = player1.getLocation();
        Location loc2 = player2.getLocation();

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("start", "arena"), allPerms));

        Assertions.assertTrue(manager.isPartyQueued(party1.getId()));

        TestEntityFactory.TestWorld world = (TestEntityFactory.TestWorld) EntityFactory.getInstance().getServer().getWorld(manager.getQueuedWorld(party1.getId()));

        try {
            Thread.sleep(16 * 1000);
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

        Assertions.assertTrue(manager.processCommand(player1, "raids", Arrays.asList("exit"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertNotEquals(loc2, player2.getLocation());

        Assertions.assertTrue(manager.processCommand(player2, "raids", Arrays.asList("exit"), allPerms));

        Assertions.assertEquals(loc1, player1.getLocation());
        Assertions.assertEquals(loc2, player2.getLocation());
    }
}
