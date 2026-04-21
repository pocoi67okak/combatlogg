package ru.saita.combatlogg;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatLogPlugin extends JavaPlugin {
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        loadConfig();

        combatManager = new CombatManager(this);
        combatManager.start();

        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager), this);

        CombatCommand combatCommand = new CombatCommand(this, combatManager);
        PluginCommand command = getCommand("combatlogg");
        if (command != null) {
            command.setExecutor(combatCommand);
            command.setTabCompleter(combatCommand);
        }
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.shutdown();
        }
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public String message(String key, String... replacements) {
        String text = getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return color(text);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        migrateConfig();
        saveConfig();
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadPluginConfig();
    }

    private void migrateConfig() {
        String path = "messages.command-blocked";
        String oldDefault = "&cВо время PvP можно использовать только /login и /register.";
        String newDefault = "&cВо время ПВП нельзя использовать команды!";

        String current = getConfig().getString(path);
        if (current == null || oldDefault.equals(current)) {
            getConfig().set(path, newDefault);
        }
    }
}
