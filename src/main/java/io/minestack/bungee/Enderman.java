package io.minestack.bungee;


import com.mongodb.ServerAddress;
import com.rabbitmq.client.Address;
import io.minestack.doublechest.DoubleChest;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubExchanges;
import io.minestack.doublechest.databases.rabbitmq.pubsub.PubSubPublisher;
import io.minestack.doublechest.model.bungee.Bungee;
import io.minestack.doublechest.model.server.Server;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Enderman extends Plugin {

    public Bungee getMinestackBungee() {
        return DoubleChest.INSTANCE.getMongoDatabase().getBungeeRepository().getModel(new ObjectId(System.getenv("bungee_id")));
    }

    private ServerStartSubscriber serverStartSubscriber;
    private ServerStopSubscriber serverStopSubscriber;

    @Override
    public void onEnable() {
        getProxy().getScheduler().runAsync(this, () -> {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    getLogger().info("Killing Bungee");
                    getProxy().stop();
                }
            });

            getLogger().info("Init Mongo Database");
            List<ServerAddress> addresses = new ArrayList<>();
            String mongoAddresses = System.getenv("mongo_addresses");
            for (String mongoAddress : mongoAddresses.split(",")) {
                String[] split = mongoAddress.split(":");
                int port = 27017;
                if (split.length == 2) {
                    port = Integer.parseInt(split[1]);
                }
                try {
                    addresses.add(new ServerAddress(split[0], port));
                } catch (UnknownHostException e) {
                    getLogger().log(Level.SEVERE, "Threw a UnknownHostException in Enderman::onEnable, full stack trace follows: ", e);
                }
            }
            if (System.getenv("mongo_username") == null) {
                DoubleChest.INSTANCE.initMongoDatabase(addresses, System.getenv("mongo_database"));
            } else {
                DoubleChest.INSTANCE.initMongoDatabase(addresses, System.getenv("mongo_username"), System.getenv("mongo_password"), System.getenv("mongo_database"));
            }

            getLogger().info("Init RabbitMQ");
            List<Address> addressList = new ArrayList<>();
            String rabbitAddresses = System.getenv("rabbit_addresses");
            for (String rabbitAddress : rabbitAddresses.split(",")) {
                String[] split = rabbitAddress.split(":");
                int port = 5672;
                if (split.length == 2) {
                    port = Integer.parseInt(split[1]);
                }
                addressList.add(new Address(split[0], port));
            }
            DoubleChest.INSTANCE.initRabbitMQDatabase(addressList, System.getenv("rabbit_username"), System.getenv("rabbit_password"));

            if (getMinestackBungee() == null) {
                getLogger().severe("Could not find bungee data");
                getProxy().stop();
                return;
            }

            getProxy().getServers().clear();
            getProxy().setReconnectHandler(new ReconnectHandler(this));

            try {
                serverStartSubscriber = new ServerStartSubscriber(this);
                serverStopSubscriber = new ServerStopSubscriber(this);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Threw a UnknownHostException in Enderman::onEnable, full stack trace follows: ", e);
            }

            new PlayerListener(this);

            getProxy().getScheduler().schedule(this, () -> {
                Bungee bungee = getMinestackBungee();
                if (bungee == null) {
                    getLogger().severe("Couldn't find bungee data stopping bungee");
                    getProxy().stop();
                    return;
                }

                if (bungee.getNetwork() == null) {
                    getLogger().severe("Couldn't find network data stopping bungee");
                    getProxy().stop();
                    return;
                }

                if (bungee.getNode() == null) {
                    getLogger().severe("Couldn't find node data stopping bungee");
                    getProxy().stop();
                    return;
                }

                if (bungee.getPublicAddress() == null) {
                    getLogger().severe("Couldn't find address data stopping bungee");
                    getProxy().stop();
                    return;
                }

                if (bungee.getBungeeType() == null) {
                    getLogger().severe("Couldn't find type data stopping bungee");
                    getProxy().stop();
                    return;
                }

                boolean newBungee = false;

                bungee.setUpdated_at(new Date(System.currentTimeMillis()));
                DoubleChest.INSTANCE.getMongoDatabase().getBungeeRepository().saveModel(bungee);

                if (newBungee == true) {
                    try {
                        PubSubPublisher pubSubPublisher = new PubSubPublisher(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.BUNGEE_START.name());

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("bungee", bungee.getId().toString());

                        pubSubPublisher.publish(jsonObject);
                        pubSubPublisher.close();
                    } catch (IOException e) {
                        getLogger().log(Level.SEVERE, "Threw a IOException in Enderman::onEnable::asyncTask, full stack trace follows: ", e);
                    }
                }

                List<ServerInfo> toRemove = new ArrayList<>();

                for (ServerInfo serverInfo : getProxy().getServers().values()) {
                    Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(new ObjectId(serverInfo.getName()));
                    if (server == null || server.getUpdated_at().getTime() == 0L) {
                        toRemove.add(serverInfo);
                    }
                }

                for (ServerInfo serverInfo : toRemove) {
                    Server server = DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getModel(new ObjectId(serverInfo.getName()));
                    for (ProxiedPlayer player : serverInfo.getPlayers()) {
                        player.connect(((ReconnectHandler) getProxy().getReconnectHandler()).getStoredServer(player));
                    }
                    if (server != null) {
                        if (server.getServerType() != null) {
                            getLogger().info("Removing Server " + server.getServerType().getName() + " from subscriber");
                        }
                    }
                    getProxy().getServers().remove(serverInfo.getName());
                }

                DoubleChest.INSTANCE.getMongoDatabase().getServerRepository().getNetworkServers(getMinestackBungee().getNetwork())
                        .stream().filter(server -> getProxy().getServerInfo(server.getId().toString()) == null).forEach(server -> {
                    if (server.getServerType() != null && server.getNode() != null) {
                        getLogger().info("Adding Server " + server.getServerType().getName() + " from loop");
                        getProxy().constructServerInfo(server.getId().toString(), new InetSocketAddress(server.getNode().getPrivateAddress(), server.getPort()), "", false);
                    }
                });
            }, 10, 10, TimeUnit.SECONDS);
        });
    }

    @Override
    public void onDisable() {
        getProxy().getScheduler().cancel(this);
        Bungee bungee = getMinestackBungee();
        if (bungee != null) {
            bungee.setUpdated_at(new Date(0));
            DoubleChest.INSTANCE.getMongoDatabase().getBungeeRepository().saveModel(bungee);
        }
        try {
            PubSubPublisher pubSubPublisher = new PubSubPublisher(DoubleChest.INSTANCE.getRabbitMQDatabase(), PubSubExchanges.BUNGEE_STOP.name());

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("bungee", System.getenv("bungee_id"));

            pubSubPublisher.publish(jsonObject);
            pubSubPublisher.close();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Threw a IOException in Enderman::onDisable, full stack trace follows: ", e);
        }

        serverStartSubscriber.stopSubscribing();
        serverStopSubscriber.stopSubscribing();
    }

}
