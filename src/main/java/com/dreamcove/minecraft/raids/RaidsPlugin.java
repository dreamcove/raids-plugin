package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.EntityFactory;
import com.dreamcove.minecraft.raids.api.MessageReceiver;
import com.dreamcove.minecraft.raids.api.PartyFactory;
import com.dreamcove.minecraft.raids.impl.PartiesPartyFactory;
import com.dreamcove.minecraft.raids.impl.PluginEntityFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RaidsPlugin extends JavaPlugin {


    private RaidsManager manager;

    public RaidsPlugin() {
        super();
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

    private List<String> getPermissions(CommandSender sender) {
        return RaidsManager.ALL_COMMANDS.stream()
                .map(RaidsManager::getPermission)
                .filter(sender::hasPermission)
                .collect(Collectors.toList());
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

        try {
            manager = new RaidsManager(new File(getDataFolder(), "config.yml").toURI().toURL(), getLogger());

            EntityFactory.getInstance().getServer().scheduleRunnable(() -> manager.cleanRaids(), 60 * 20);
        } catch (Exception exc) {
            getLogger().severe("Error loading config file");
            getLogger().throwing("RaidsPlugin", "onEnable", exc);
        }
    }
}