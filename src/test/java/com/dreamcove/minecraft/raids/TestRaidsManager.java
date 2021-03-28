package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
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

public class TestRaidsManager {
    private static RaidsManager manager;

    @BeforeAll
    public static void load() {
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

        TestEntityFactory.TestServer server = (TestEntityFactory.TestServer) EntityFactory.getInstance().getServer();
        server.addPlayer(new TestEntityFactory.TestPlayer("123"));
        server.addPlayer(new TestEntityFactory.TestPlayer("456"));

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

        List<String> perms = Arrays.asList(
                RaidsManager.PERM_CANCEL_RAID,
                RaidsManager.PERM_END_RAID,
                RaidsManager.PERM_EXIT_RAID,
                RaidsManager.PERM_RELOAD,
                RaidsManager.PERM_START_RAID);

        Assertions.assertEquals(5, manager.getHelp(perms).size());
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
}
