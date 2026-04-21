package ru.saita.combatlogg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public final class CombatCommand implements CommandExecutor, TabCompleter {
    private final CombatLogPlugin plugin;
    private final CombatManager combatManager;

    public CombatCommand(CombatLogPlugin plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("combatlogg.admin")) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("time".equals(subCommand)) {
            return setTime(sender, args);
        }
        if ("elytra".equals(subCommand)) {
            return setElytra(sender, args);
        }
        if ("command".equals(subCommand)) {
            return setCommandBlock(sender, args);
        }
        if ("reload".equals(subCommand)) {
            plugin.reloadPluginConfig();
            combatManager.reloadSettings();
            sender.sendMessage(plugin.message("reload"));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("combatlogg.admin")) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], list("time", "elytra", "command", "reload"), suggestions);
        } else if (args.length == 2 && "time".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], list("30", "60", "120"), suggestions);
        } else if (args.length == 2 && ("elytra".equalsIgnoreCase(args[0]) || "command".equalsIgnoreCase(args[0]))) {
            StringUtil.copyPartialMatches(args[1], list("on", "off"), suggestions);
        }

        Collections.sort(suggestions);
        return suggestions;
    }

    private boolean setTime(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(plugin.message("time-usage"));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.message("time-invalid"));
            return true;
        }

        if (seconds < 1 || seconds > 86400) {
            sender.sendMessage(plugin.message("time-invalid"));
            return true;
        }

        plugin.getConfig().set("combat-time-seconds", seconds);
        plugin.saveConfig();
        combatManager.reloadSettings();
        sender.sendMessage(plugin.message("time-set", "{time}", Integer.toString(seconds)));
        return true;
    }

    private boolean setElytra(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(plugin.message("elytra-usage"));
            return true;
        }

        String value = args[1].toLowerCase(Locale.ROOT);
        if ("on".equals(value)) {
            plugin.getConfig().set("block-elytra-in-pvp", true);
            plugin.saveConfig();
            combatManager.reloadSettings();
            sender.sendMessage(plugin.message("elytra-set-on"));
            return true;
        }
        if ("off".equals(value)) {
            plugin.getConfig().set("block-elytra-in-pvp", false);
            plugin.saveConfig();
            combatManager.reloadSettings();
            sender.sendMessage(plugin.message("elytra-set-off"));
            return true;
        }

        sender.sendMessage(plugin.message("elytra-usage"));
        return true;
    }

    private boolean setCommandBlock(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(plugin.message("command-usage"));
            return true;
        }

        String value = args[1].toLowerCase(Locale.ROOT);
        if ("on".equals(value)) {
            plugin.getConfig().set("block-commands-in-pvp", true);
            plugin.saveConfig();
            combatManager.reloadSettings();
            sender.sendMessage(plugin.message("command-set-on"));
            return true;
        }
        if ("off".equals(value)) {
            plugin.getConfig().set("block-commands-in-pvp", false);
            plugin.saveConfig();
            combatManager.reloadSettings();
            sender.sendMessage(plugin.message("command-set-off"));
            return true;
        }

        sender.sendMessage(plugin.message("command-usage"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.getConfig().getStringList("messages.help")) {
            sender.sendMessage(plugin.color(line));
        }
    }

    private List<String> list(String... values) {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, values);
        return result;
    }
}
