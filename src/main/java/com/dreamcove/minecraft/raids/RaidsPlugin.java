package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.MessageReceiver;
import com.dreamcove.minecraft.raids.api.PartyFactory;
import com.dreamcove.minecraft.raids.impl.PartiesPartyFactory;
import com.dreamcove.minecraft.raids.impl.PluginEntityFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RaidsPlugin extends JavaPlugin {


    private RaidsManager manager;

    public RaidsPlugin() {
        super();
    }

    private List<String> getPermissions(CommandSender sender) {
        return Stream.of(
                        RaidsManager.PERM_CANCEL_RAID,
                        RaidsManager.PERM_END_RAID,
                        RaidsManager.PERM_EXIT_RAID,
                        RaidsManager.PERM_START_RAID,
                        RaidsManager.PERM_RELOAD
                )
                        .filter(sender::hasPermission)
                        .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageReceiver receiver;

        if (sender instanceof org.bukkit.entity.Player) {
            receiver = EntityFactory.getInstance().wrap((org.bukkit.entity.Player) sender);
        } else {
            receiver = sender::sendMessage;
        }

        return manager.processCommand(receiver, command.getName(), Arrays.asList(args), getPermissions(sender));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return manager.getTabComplete(command.getName(), getPermissions(sender), Arrays.asList(args));
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
}