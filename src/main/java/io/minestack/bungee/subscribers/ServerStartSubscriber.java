package io.minestack.bungee.subscribers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import io.minestack.bungee.Enderman;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubExchanges;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubSubscriber;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.api.config.ServerInfo;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerStartSubscriber extends PubSubSubscriber {

    private final Enderman plugin;

    public ServerStartSubscriber(Enderman plugin) throws IOException {
        super(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.SERVER_START.name());
        this.plugin = plugin;
    }

    @Override
    public void messageDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        JSONObject jsonObject = new JSONObject(new String(bytes));

        ObjectId serverId = new ObjectId(jsonObject.getString("server"));

        Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(serverId);

        if (server == null) {
            return;
        }
        if (server.getNode() == null) {
            return;
        }
        if (server.getServerType() == null) {
            return;
        }
        if (server.getPort() == 0) {
            return;
        }
        if (server.getUpdated_at().getTime() == 0L) {
            return;
        }
        if (plugin.getProxy().getServers().containsKey(server.getId().toString()) == true) {
            return;
        }
        plugin.getLogger().info("Adding Server "+server.getServerType().getName()+"."+server.getNumber()+" from subscriber");
        ServerInfo serverInfo = plugin.getProxy().constructServerInfo(server.getId().toString(), new InetSocketAddress(server.getNode().getPrivateAddress(), server.getPort()), "", false);
        plugin.getProxy().getServers().put(serverInfo.getName(), serverInfo);
    }
}
