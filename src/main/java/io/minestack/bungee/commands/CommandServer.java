package io.minestack.bungee.commands;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.minestack.bungee.Enderman;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.model.bungee.Bungee;
import io.minestack.doublechest.model.pluginhandler.servertype.ServerType;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.Map;

public class CommandServer extends Command implements TabExecutor {

    private final Enderman plugin;

    public CommandServer(Enderman plugin) {
        super("server", "bungeecord.command.server");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;
        Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
        if (args.length == 0) {
            String serverName;
            try {
                Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(new ObjectId(player.getServer().getInfo().getName()));
                if (server == null) {
                    throw new Exception();
                }
                if (server.getServerType() == null) {
                    throw new Exception();
                }
                serverName = server.getServerType().getName()+"."+server.getNumber();
            } catch (Exception ex) {
                serverName = player.getServer().getInfo().getName();
            }
            player.sendMessage(ProxyServer.getInstance().getTranslation("current_server", serverName));
            TextComponent serverList = new TextComponent(ProxyServer.getInstance().getTranslation("server_list"));
            serverList.setColor(ChatColor.GOLD);
            boolean first = true;
            for (ServerInfo serverInfo : servers.values()) {
                if (serverInfo.canAccess(player)) {
                    TextComponent serverTextComponent;
                    try {
                        Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(new ObjectId(serverInfo.getName()));
                        if (server == null) {
                            throw new Exception();
                        }
                        if (server.getServerType() == null) {
                            throw new Exception();
                        }
                        serverTextComponent = new TextComponent(first ? server.getServerType().getName() + "." + server.getNumber() : ", " + server.getServerType().getName() + "." + server.getNumber());
                    } catch (Exception ex) {
                        serverTextComponent = new TextComponent(first ? serverInfo.getName() : ", " + serverInfo.getName());
                    }
                    serverTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(serverInfo.getAddress().getHostName()+":"+serverInfo.getAddress().getPort()+"\n")
                                    .append("Click to connect to the server").italic(true)
                                    .create()));
                    serverTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + serverInfo.getName()));
                    serverList.addExtra(serverTextComponent);
                    first = false;
                }
            }
            player.sendMessage(serverList);
        } else {
            ServerInfo serverInfo = servers.get(args[0]);
            if (serverInfo == null) {
                String[] info = args[0].split("\\.");
                Bungee bungee = plugin.getMinestackBungee();
                if (bungee == null) {
                    player.sendMessage("Unknown Bungee");
                    return;
                }
                if (bungee.getNetwork() == null) {
                    player.sendMessage("Unknown Network");
                    return;
                }
                ServerType serverType = DoubleChest.INSTANCE.getMongoDatabase().getServerTypeRepository().getModel(info[0]);
                if (serverType == null) {
                    player.sendMessage(ProxyServer.getInstance().getTranslation("no_server"));
                    return;
                }
                Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getNetworkServerTypeServerNumber(bungee.getNetwork(),
                        serverType, Integer.parseInt(info[1]));
                if (server == null) {
                    player.sendMessage(ProxyServer.getInstance().getTranslation("no_server"));
                    return;
                }
                serverInfo = servers.get(server.getId().toString());
                if (serverInfo == null) {
                    player.sendMessage(ProxyServer.getInstance().getTranslation("no_server"));
                    return;
                }
            }
            if (!serverInfo.canAccess(player)) {
                player.sendMessage(ProxyServer.getInstance().getTranslation("no_server_permission"));
                return;
            }
            player.connect(serverInfo);
        }
    }

    @Override
    public Iterable<String> onTabComplete(final CommandSender sender, String[] args) {
        return (args.length != 0) ? Collections.EMPTY_LIST : Iterables.transform(Iterables.filter(DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModels(), new Predicate<Server>() {
            @Override
            public boolean apply(Server input) {
                ServerInfo server = plugin.getProxy().getServerInfo(input.getId().toString());
                if (server != null) {
                    return server.canAccess(sender);
                } else {
                    return false;
                }
            }
        }), new Function<Server, String>() {
            @Override
            public String apply(Server input) {
                if (input.getServerType() != null) {
                    return input.getServerType().getName() + "." + input.getNumber();
                } else {
                    return input.getId().toString();
                }
            }
        });
    }

}
