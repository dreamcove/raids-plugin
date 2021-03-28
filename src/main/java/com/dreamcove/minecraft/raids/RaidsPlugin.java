package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.*;
import com.dreamcove.minecraft.raids.impl.PartiesPartyFactory;
import com.dreamcove.minecraft.raids.impl.PluginEntityFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RaidsPlugin extends JavaPlugin {


    public RaidsPlugin() {
        super();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        EntityFactory.setInstance(new PluginEntityFactory(this));
        if (getServer().getPluginManager().getPlugin("Parties") != null) {
            PartyFactory.setInstance(new PartiesPartyFactory());
        }

        manager = new RaidsManager(getLogger());

        EntityFactory.getInstance().getServer().scheduleRunnable(() -> manager.cleanRaids(), 60 * 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageReceiver receiver;

        if (sender instanceof org.bukkit.entity.Player) {
            receiver = EntityFactory.getInstance().wrap((org.bukkit.entity.Player)sender);
        } else {
            receiver = sender::sendMessage;
        }

        return manager.processCommand(receiver, command.getName(), Arrays.asList(args));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> perms =
                Stream.of(
                    RaidsManager.PERM_CANCEL_RAID,
                    RaidsManager.PERM_END_RAID,
                    RaidsManager.PERM_EXIT_RAID,
                    RaidsManager.PERM_START_RAID,
                    RaidsManager.PERM_RELOAD
                )
                .filter(sender::hasPermission)
                .collect(Collectors.toList());

        return manager.getTabComplete(command.getName(), perms, Arrays.asList(args));
    }

    private RaidsManager manager;
}