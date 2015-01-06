package io.minestack.bungee;

import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.model.bungee.Bungee;
import io.minestack.doublechest.model.network.Network;
import io.minestack.doublechest.model.pluginhandler.servertype.ServerType;
import io.minestack.doublechest.model.server.Server;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ReconnectHandler extends AbstractReconnectHandler {

    private final Enderman plugin;

    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        ServerInfo serverInfo = ReconnectHandler.getForcedHost(player.getPendingConnection());
        plugin.getLogger().info("Forced Host " + serverInfo);
        if (serverInfo == null) {
            serverInfo = getDefault(player);
            plugin.getLogger().info("Default Host " + serverInfo);
        }
        if (serverInfo == null) {
            player.disconnect(new TextComponent("Unable to find a server to connect to. Please report."));
        }
        return serverInfo;
    }

    @Override
    public ServerInfo getStoredServer(ProxiedPlayer proxiedPlayer) {
        return getDefault(proxiedPlayer);
    }

    @Override
    public void setServer(ProxiedPlayer proxiedPlayer) {

    }

    private ServerInfo getDefault(ProxiedPlayer proxiedPlayer) {
        String defaultServer = proxiedPlayer.getPendingConnection().getListener().getDefaultServer();
        Server server;
        if (proxiedPlayer.getServer() == null) {
            server = getServerWithRoom(plugin, new ObjectId(defaultServer));
        } else {
            server = getServerWithRoom(plugin, new ObjectId(defaultServer), proxiedPlayer.getServer().getInfo());
        }
        if (server == null) {
            plugin.getLogger().severe("Null server with room");
            return null;
        }
        ServerInfo serverInfo = plugin.getProxy().getServerInfo(server.getId().toString());
        if (serverInfo == null) {
            plugin.getLogger().warning("Null server info");
        }
        return serverInfo;
    }

    public static ServerInfo getForcedHost(PendingConnection connection) {
        Enderman plugin = (Enderman) ProxyServer.getInstance().getPluginManager().getPlugin("Enderman");
        if (connection.getVirtualHost() == null) {
            return null;
        }
        String forced = connection.getListener().getForcedHosts().get(connection.getVirtualHost().getHostString());
        if (forced == null) {
            return null;
        }

        Server server = getServerWithRoom(plugin, new ObjectId(forced));

        if (server == null) {
            return null;
        }

        return plugin.getProxy().getServerInfo(server.getId().toString());
    }

    public static Server getServerWithRoom(Enderman plugin, ObjectId serverTypeId) {
        return getServerWithRoom(plugin, serverTypeId, null);
    }

    private static Server getServerWithRoom(Enderman plugin, ObjectId serverTypeId, ServerInfo lastServer) {
        ServerType serverType = DoubleChest.INSTANCE.getMongoDatabase().getServerTypeRepository().getModel(serverTypeId);

        if (serverType == null) {
            plugin.getLogger().info("Unknown server type " + serverTypeId.toString());
            return null;
        }

        Bungee bungee = plugin.getMinestackBungee();

        if (bungee == null) {
            plugin.getLogger().info("Unknown bungee");
            return null;
        }

        Network network = bungee.getNetwork();

        if (network == null) {
            plugin.getLogger().info("Unknown network");
            return null;
        }

        List<Server> servers = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getNetworkServerTypeServers(network, serverType);

        if (servers.isEmpty()) {
            plugin.getLogger().info("No servers with type of " + serverType.getName());
            return null;
        }

        ArrayList<Server> roomServers = new ArrayList<>();

        for (Server server : servers) {
            if (server.getPort() == 0 || server.getUpdated_at().getTime() == 0L) {
                continue;
            }
            if (plugin.getProxy().getServerInfo(server.getId().toString()) != null) {
                if ((server.getServerType().getPlayers() - server.getPlayers()) > 0) {
                    if (lastServer != null) {
                        if (server.getId().toString().equals(lastServer.getName())) {
                            continue;
                        }
                    }
                    roomServers.add(server);
                }
            }
        }

        if (roomServers.isEmpty()) {
            plugin.getLogger().info("Cannot find a empty server "+serverType.getName());
            return null;
        }
        int random = (int) (Math.random() * roomServers.size());
        return roomServers.get(random);
    }

    @Override
    public void save() {

    }

    @Override
    public void close() {

    }
}
