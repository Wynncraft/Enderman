package io.minestack.bungee.subscribers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import io.minestack.bungee.Enderman;
import io.minestack.bungee.ReconnectHandler;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubExchanges;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubSubscriber;
import io.minestack.doublechest.model.pluginhandler.servertype.ServerType;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;

public class TeleportSubscriber extends PubSubSubscriber {

    private final Enderman plugin;

    public TeleportSubscriber(Enderman plugin) throws IOException {
        super(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.TELEPORT.name());
        this.plugin = plugin;
    }

    @Override
    public void messageDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        JSONObject jsonObject = new JSONObject(new String(bytes));

        if (jsonObject.has("player") == false && jsonObject.has("players") == false) {
            plugin.getLogger().info("No player or players in teleport message");
            return;
        }

        if (jsonObject.has("serverType")) {
            ObjectId serverTypeId;
            try {
                serverTypeId = new ObjectId(jsonObject.getString("serverType"));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Threw a Exception in TeleportSubscriber::messageDelivery, full stack trace follows: ", e);
                return;
            }

            ServerType serverType = DoubleChest.INSTANCE.getMongoDatabase().getServerTypeRepository().getModel(serverTypeId);

            if (serverType == null) {
                plugin.getLogger().warning("Unknown server type in teleport message");
                return;
            }

            Server server = ReconnectHandler.getServerWithRoom(plugin, serverType.getId());

            if (server == null) {
                plugin.getLogger().warning("Could not find server from server type in teleport message");
                return;
            }

            ServerInfo serverInfo = plugin.getProxy().getServerInfo(server.getId().toString());

            if (serverInfo == null) {
                plugin.getLogger().warning("Unknown server info from server type in teleport message");
                return;
            }

            if (jsonObject.has("player")) {
                ProxiedPlayer player = plugin.getProxy().getPlayer(jsonObject.getString("player"));

                if (player != null) {
                    player.connect(serverInfo);
                }
            } else {
                JSONArray jsonArray = jsonObject.getJSONArray("players");
                for (int i = 0; i < jsonArray.length(); i++) {
                    String playerName = jsonArray.getString(i);
                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);

                    if (player != null) {
                        player.connect(serverInfo);
                    }
                }
            }
        } else if (jsonObject.has("server")) {
            ObjectId serverId;
            try {
                serverId = new ObjectId(jsonObject.getString("server"));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Threw a Exception in TeleportSubscriber::messageDelivery, full stack trace follows: ", e);
                return;
            }

            Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(serverId);

            if (server == null) {
                plugin.getLogger().warning("Unknown server in teleport message");
                return;
            }

            ServerInfo serverInfo = plugin.getProxy().getServerInfo(server.getId().toString());

            if (serverInfo == null) {
                plugin.getLogger().warning("Unknown server info in teleport message");
                return;
            }

            if (jsonObject.has("player")) {
                ProxiedPlayer player = plugin.getProxy().getPlayer(jsonObject.getString("player"));

                if (player != null) {
                    player.connect(serverInfo);
                }
            } else {
                JSONArray jsonArray = jsonObject.getJSONArray("players");
                for (int i = 0; i < jsonArray.length(); i++) {
                    String playerName = jsonArray.getString(i);
                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);

                    if (player != null) {
                        player.connect(serverInfo);
                    }
                }
            }
        } else {
            plugin.getLogger().info("No serverType or server field in teleport message");
        }
    }
}
