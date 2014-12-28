package io.minestack.bungee;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubExchanges;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubSubscriber;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.IOException;

public class ServerStopSubscriber extends PubSubSubscriber {

    private final Enderman plugin;

    public ServerStopSubscriber(Enderman plugin) throws IOException {
        super(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.SERVER_START.name());
        this.plugin = plugin;
    }

    @Override
    public void messageDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        JSONObject jsonObject = new JSONObject(bytes);

        ObjectId serverId = new ObjectId(jsonObject.getString("server"));

        Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(serverId);

        if (server == null) {
            return;
        }

        if (server.getServerType() == null) {
            return;
        }

        ServerInfo serverInfo = plugin.getProxy().getServerInfo(server.getId().toString());

        if (serverInfo != null) {
            for (ProxiedPlayer player : serverInfo.getPlayers()) {
                player.connect(((ReconnectHandler) plugin.getProxy().getReconnectHandler()).getStoredServer(player));
            }

            plugin.getLogger().info("Removing Server "+server.getServerType().getName()+" from subscriber");
            plugin.getProxy().getServers().remove(server.getId().toString());
        }
    }
}
