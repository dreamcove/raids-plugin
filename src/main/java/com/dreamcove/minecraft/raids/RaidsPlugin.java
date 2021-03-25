package com.dreamcove.minecraft.raids;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
            String worldName = getQueuedWorld(partyId);
            if (worldName != null) {
                MultiverseCore core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
                Party party = Parties.getApi().getParty(partyId);

                getLogger().info("Sending party " + party.getName() + " to " + getQueuedWorld(partyId));

                MultiverseWorld world = core.getMVWorldManager().getMVWorld(getQueuedWorld(partyId));

                party.getMembers().stream().forEach(u -> core.teleportPlayer(sender, getServer().getPlayer(u), world.getSpawnLocation()));

                queuedParties.remove(partyId);
            }
        }

        int secondsRemaining;
        UUID partyId;
        CommandSender sender;
    }


    public RaidsPlugin() {
        super();
    }

    private void queueParty(CommandSender sender, UUID partyId, String worldName) {
        queuedParties.put(partyId, worldName);

        Party party = Parties.getApi().getParty(partyId);

        party.broadcastMessage("Party is queued for a raid.", null);
        party.broadcastMessage("Starting in 15 seconds.", null);
        party.broadcastMessage("Use /raids cancel to abort.", null);

        new CountdownRunnable(sender, partyId, 15).runTaskLater(this, 15 * 20);
    }

    private String getQueuedWorld(UUID partyId) {
        return queuedParties.get(partyId);
    }

    private boolean cancelRaid(UUID partyId) {
        if (queuedParties.keySet().contains(partyId)) {
            queuedParties.remove(partyId);

            Parties.getApi().getParty(partyId).broadcastMessage("Raid canceled", null);

            return true;
        }

        return false;
    }



    @Override
    public void onEnable() {
        super.onEnable();

        new BukkitRunnable() {
            @Override
            public void run() {
                MultiverseCore core = (MultiverseCore)getServer().getPluginManager().getPlugin("Multiverse-Core");

                for (Iterator<MultiverseWorld> i = core.getMVWorldManager().getMVWorlds().iterator(); i.hasNext();) {
                    MultiverseWorld w = i.next();

                    if (!queuedParties.values().contains(w.getName())) {
                        if (w.getName().startsWith("raid_")) {
                            if (w.getCBWorld().getPlayers().size() == 0) {
                                getLogger().info("Removing unused dungeon - " + w.getName() + " (no players)");

                                core.getMVWorldManager().deleteWorld(w.getName());
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20, 20 * 15);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equals("raids")) {
            if (args.length >= 1) {
                Player player = null;

                if (sender instanceof Player) {
                    player = (Player) sender;
                } else if (args.length >= 1) {
                    player = getServer().getPlayer(args[args.length - 1]);

                    if (player == null) {
                        sender.sendMessage("Player not specified as last parameter");
                    }
                }

                if (player != null) {
                    switch (label) {
                        case "start":
                            if (args.length == 2) {
                                MultiverseCore core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
                                PartiesAPI parties = Parties.getApi();

                                // Find Party
                                final UUID playerId = player.getUniqueId();
                                List<Party> foundParties = parties.getOnlineParties()
                                        .stream()
                                        .filter(p -> p.getMembers().contains(playerId))
                                        .collect(Collectors.toList());
                                if (foundParties.size() == 0) {
                                    sender.sendMessage("Player must belong to party");
                                } else {
                                    Party party = foundParties.get(0);
                                    boolean found = false;
                                    for (Iterator<MultiverseWorld> i = core.getMVWorldManager().getMVWorlds().iterator(); i.hasNext(); ) {
                                        MultiverseWorld w = i.next();

                                        if (w.getName().equals("template_" + args[0])) {
                                            sender.sendMessage("Creating raid dungeon");

                                            String newWorld = "raid_" + args[0] + "_" + System.currentTimeMillis();

                                            queuedParties.put(party.getId(), newWorld);

                                            if (core.getMVWorldManager().cloneWorld(w.getName(), newWorld)) {
                                                queueParty(sender, party.getId(), newWorld);
                                            }

                                            found = true;
                                        }
                                    }

                                    if (!found) {
                                        sender.sendMessage("Could not find raid template");
                                    }
                                }
                            } else {
                                sender.sendMessage("/startraid requires 2 arguments");
                            }

                            return true;
                        case "cancel":
                            // Check to see if calling entity is a player
                            if (sender instanceof Player) {
                                PartiesAPI parties = Parties.getApi();

                                final UUID playerId = player.getUniqueId();
                                List<Party> foundParties = parties.getOnlineParties()
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
                    }
                }
            } else {
                sender.sendMessage("Command /raids requires at least one parameter");
            }
            return true;
        }

        return false;
    }

    private Map<UUID, String> queuedParties = Collections.synchronizedMap(new HashMap<UUID, String>());
}