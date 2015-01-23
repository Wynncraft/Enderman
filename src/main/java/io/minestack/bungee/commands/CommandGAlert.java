package io.minestack.bungee.commands;

import io.minestack.bungee.Enderman;
import io.minestack.doublechest.databases.rabbitmq.publishers.GlobalMessagePublisher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.json.JSONObject;

import java.io.IOException;

public class CommandGAlert extends Command {

    private final Enderman plugin;

    public CommandGAlert(Enderman plugin) {
        super("galert", "bungeecord.command.alert");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (args.length == 0) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED+"Usage: /galert [message]"));
            return;
        }

        String message = "";
        for (String s : args) {
            message += s+" ";
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", message);

        try {
            new GlobalMessagePublisher().publish(jsonObject);
        } catch (IOException e) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED+"Error sending global message."));
        }
    }
}
