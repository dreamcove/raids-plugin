package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import com.dreamcove.minecraft.raids.impl.PartiesPartyFactory;
import com.dreamcove.minecraft.raids.impl.PluginEntityFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class RaidsPlugin extends JavaPlugin {
    class CountdownRunnable extends BukkitRunnable {
        public CountdownRunnable(CommandSender sender, UUID partyId, int secondsRemaining) {
            super();

            this.secondsRemaining = secondsRemaining;
            this.partyId = partyId;
            this.sender = sender;
        }

        @Override
        public void run() {
            String worldName = manager.getQueuedWorld(partyId);
            if (worldName != null) {
                Party party = PartyFactory.getInstance().getParty(partyId);

                getLogger().info("Sending party " + party.getName() + " to " + manager.getQueuedWorld(partyId));

                World world = EntityFactory.getInstance().getServer().getWorld(manager.getQueuedWorld(partyId));

                party.getMembers().stream()
                        .map(u -> getServer().getPlayer(u))
                        .map(p -> EntityFactory.getInstance().wrap(p))
                        .forEach(p -> {
                            manager.storeLastLocation(p);
                            p.teleport(world.getSpawnLocation());
                        });

                manager.dequeueParty(partyId);
            }
        }

        int secondsRemaining;
        UUID partyId;
        CommandSender sender;
    }


    public RaidsPlugin() {
        super();
    }

    private void startRaid(CommandSender sender, UUID partyId, String oldWorld, String newWorld) throws IOException {
        manager.queueParty(partyId, newWorld);

        try {
            manager.cloneWorld(oldWorld, newWorld);

            Party party = PartyFactory.getInstance().getParty(partyId);

            party.broadcastMessage("Party is queued for a raid.");
            party.broadcastMessage("Starting in 15 seconds.");
            party.broadcastMessage("Use /raids cancel to abort.");

            new CountdownRunnable(sender, partyId, 15).runTaskLater(this, 15 * 20);
        } catch (IOException e) {
            manager.dequeueParty(partyId);
            throw e;
        }
    }

    private boolean cancelRaid(UUID partyId) {
        if (manager.isPartyQueued(partyId)) {
            manager.dequeueParty(partyId);

            PartyFactory.getInstance().getParty(partyId).broadcastMessage("Raid canceled");

            return true;
        }

        return false;
    }



    @Override
    public void onEnable() {
        super.onEnable();

        EntityFactory.setInstance(new PluginEntityFactory(this));
        if (getServer().getPluginManager().getPlugin("Parties") != null) {
            PartyFactory.setInstance(new PartiesPartyFactory());
        }

        manager = new RaidsManager(getConfig(), getLogger());

        new BukkitRunnable() {
            @Override
            public void run() {
                manager.cleanRaids();
            }
        }.runTaskTimer(this, 20, 20 * 15);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("raids")) {
            if (args.length >= 1) {
                Player player = null;

                if (sender instanceof org.bukkit.entity.Player) {
                    player = EntityFactory.getInstance().wrap((org.bukkit.entity.Player) sender);
                } else if (args.length >= 1) {
                    player = EntityFactory.getInstance().getServer().getPlayer(args[args.length - 1]);

                    if (player == null) {
                        sender.sendMessage("Player not specified as last parameter");
                    }
                }

                if (player != null) {
                    switch (args[0]) {
                        case "start":
                            if (args.length == 2) {
                                // Find Party
                                final UUID playerId = player.getUniqueId();
                                final UUID partyId = PartyFactory.getInstance().getPartyForPlayer(playerId);
                                if (partyId == null) {
                                    sender.sendMessage("Player must belong to party");
                                } else {
                                    boolean found = false;
                                    for (Iterator<World> i = EntityFactory.getInstance().getServer().getWorlds().iterator(); i.hasNext(); ) {
                                        World w = i.next();

                                        if (w.getName().equals("template_" + args[1])) {
                                            sender.sendMessage("Creating raid dungeon");

                                            String newWorld = "raid_" + args[1] + "_" + System.currentTimeMillis();

                                            try {
                                                startRaid(sender, partyId, args[1], newWorld);
                                                found = true;
                                            } catch (IOException e) {
                                                sender.sendMessage("Error creating raid");
                                                getLogger().throwing(RaidsPlugin.class.getName(), "onCommand", e);
                                            }

                                        }
                                    }

                                    if (!found) {
                                        sender.sendMessage("Could not find raid template");
                                    }
                                }
                            } else {
                                sender.sendMessage("/startraid requires 2 arguments");
                            }
                            break;
                        case "cancel":
                            // Check to see if calling entity is a player
                            if (sender instanceof Player) {
                                final UUID playerId = player.getUniqueId();
                                List<Party> foundParties = PartyFactory.getInstance().getOnlineParties()
                                        .stream()
                                        .filter(p -> p.getMembers().contains(playerId))
                                        .collect(Collectors.toList());

                                if (foundParties.size() > 0) {
                                    if (!cancelRaid(foundParties.get(0).getId())) {
                                        sender.sendMessage("Your party is not starting a raid");
                                    }
                                } else {
                                    sender.sendMessage("You must belong to party in order cancel a raid");
                                }
                            } else {
                                sender.sendMessage("Only players can cancel raids");
                            }
                            break;
                        case "exit":
                            manager.returnLastLocation(player);
                            break;
                        case "end":
                            if (manager.getLastLocation(player) != null) {
                                player
                                    .getWorld()
                                    .getEntities()
                                    .stream()
                                    .filter(e -> e instanceof org.bukkit.entity.Player)
                                    .map(e -> EntityFactory.getInstance().wrap((org.bukkit.entity.Player)e))
                                    .forEach(p -> manager.returnLastLocation(p));
                            }
                            break;
                    }
                }
            } else {
                sender.sendMessage("Command /raids requires at least one parameter");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();

        if (command != null && command.getName().equals("raids")) {
            if (args.length == 1) {
                if (sender.hasPermission("raids.start")) {
                    result.add("start");
                }
                if (sender.hasPermission("raids.cancel")) {
                    result.add("cancel");
                }
                if (sender.hasPermission("raids.exit")) {
                    result.add("exit");
                }
                if (sender.hasPermission("raids.end")) {
                    result.add("end");
                }
            } else if (args.length == 2) {
                if (args[0].equals("start") && sender.hasPermission("raids.start")) {
                    result.addAll(getServer()
                            .getWorlds()
                            .stream()
                            .filter(w -> w.getName().startsWith("template_"))
                            .map(w -> w.getName().substring(9))
                            .collect(Collectors.toList()));
                }
            }
        }

        return result;
    }

    private RaidsManager manager;
}