package io.minestack.bungee.commands;

import io.minestack.bungee.Enderman;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.model.bungee.Bungee;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandList extends Command {

    private final Enderman plugin;

    public CommandList(Enderman plugin) {
        super("glist", "bungeecord.command.list");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int onlineNetwork = 0;
        Bungee bungee = plugin.getMinestackBungee();
        if (bungee == null) {
            return;
        }
        if (bungee.getNetwork() == null) {
            return;
        }
        for (Server server : DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getNetworkServers(bungee.getNetwork(), true)) {
            onlineNetwork += server.getPlayers().size();
        }
        for (ServerInfo serverInfo : ProxyServer.getInstance().getServers().values()) {
            if (!serverInfo.canAccess(sender)) {
                continue;
            }
            List<String> players = serverInfo.getPlayers().stream().map(ProxiedPlayer::getDisplayName).collect(Collectors.toList());
            Collections.sort( players, String.CASE_INSENSITIVE_ORDER );
            try {
                Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(new ObjectId(serverInfo.getName()));
                if (server == null) {
                    throw new Exception();//to break out and show the regular server format
                }
                if (server.getServerType() != null) {
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[" + server.getServerType().getName() + "." + server.getNumber() + "] "+ChatColor.GOLD+"(" + server.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
                } else {
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[NULL." + server.getNumber() + "] "+ChatColor.GOLD+"(" + server.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
                }
            } catch (Exception ex) {
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "[" + serverInfo.getName() + "] (" + serverInfo.getPlayers().size() + "): "+ChatColor.RESET + Util.format(players, ChatColor.RESET + ", ")));
            }
        }
        sender.sendMessage(new TextComponent("Total Players on Bungee: " + plugin.getProxy().getOnlineCount()));
        sender.sendMessage(new TextComponent("Current Bungee: "+bungee.getBungeeType().getName()+" Node: "+bungee.getNode().getName()+" Address: "+bungee.getPublicAddress().getPublicAddress()));
        sender.sendMessage(new TextComponent("Total Players on Network: "+onlineNetwork));
        sender.sendMessage(new TextComponent("Current Network: "+bungee.getNetwork().getName()));
    }

}
