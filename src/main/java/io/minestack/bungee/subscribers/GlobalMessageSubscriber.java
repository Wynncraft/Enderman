package io.minestack.bungee.subscribers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import io.minestack.bungee.Enderman;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubExchanges;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubSubscriber;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.json.JSONObject;

import java.io.IOException;

public class GlobalMessageSubscriber extends PubSubSubscriber {

    private final Enderman plugin;

    public GlobalMessageSubscriber(Enderman plugin) throws IOException {
        super(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.GLOBAL_MESSAGE.name());
        this.plugin = plugin;
    }

    @Override
    public void messageDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        JSONObject jsonObject = new JSONObject(new String(bytes));

        if (jsonObject.has("message") == false) {
            plugin.getLogger().warning("No message in global message");
            return;
        }

        plugin.getProxy().broadcast(new TextComponent(ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Global Alert" + ChatColor.GRAY + "] " + ChatColor.RED + jsonObject.getString("message")));
    }
}
