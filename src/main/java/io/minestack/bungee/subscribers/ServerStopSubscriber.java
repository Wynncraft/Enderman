package io.minestack.bungee.subscribers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import io.minestack.bungee.Enderman;
import io.minestack.bungee.ReconnectHandler;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubExchanges;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubSubscriber;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;

public class ServerStopSubscriber extends PubSubSubscriber {

    private final Enderman plugin;

    public ServerStopSubscriber(Enderman plugin) throws IOException {
        super(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.SERVER_STOP.name());
        this.plugin = plugin;
    }

    @Override
    public void messageDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        JSONObject jsonObject = new JSONObject(new String(bytes));

        ObjectId serverId = null;
        try {
            serverId = new ObjectId(jsonObject.getString("server"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Threw a Exception in ServerStopSubscriber::messageDelivery, full stack trace follows: ", e);
            return;
        }

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

            plugin.getLogger().info("Removing Server "+server.getServerType().getName()+"."+server.getNumber()+" from subscriber");
            plugin.getProxy().getServers().remove(server.getId().toString());
        }
    }
}
