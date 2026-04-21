package ru.saita.combatlogg;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class CombatManager {
    private final CombatLogPlugin plugin;
    private final Map<UUID, CombatTag> tags = new HashMap<>();

    private BukkitTask task;
    private int durationSeconds;
    private boolean blockElytraInPvp;
    private boolean blockCommandsInPvp;
    private boolean actionBarEnabled;
    private String pvpBossBarTitle;
    private String pveBossBarTitle;
    private String pvpActionBar;
    private String pveActionBar;
    private BarColor pvpBossBarColor;
    private BarColor pveBossBarColor;
    private BarColor warningBossBarColor;
    private BarStyle bossBarStyle;

    public CombatManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        reloadSettings();
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        for (CombatTag tag : tags.values()) {
            tag.bar.removeAll();
        }
        tags.clear();
    }

    public void reloadSettings() {
        durationSeconds = Math.max(1, plugin.getConfig().getInt("combat-time-seconds", 30));
        blockElytraInPvp = plugin.getConfig().getBoolean("block-elytra-in-pvp", true);
        blockCommandsInPvp = plugin.getConfig().getBoolean("block-commands-in-pvp", true);
        actionBarEnabled = plugin.getConfig().getBoolean("actionbar.enabled", true);
        pvpBossBarTitle = plugin.getConfig().getString("bossbar.pvp-title",
                plugin.getConfig().getString("bossbar.title", "&4&lPvP &8| &c{time}s"));
        pveBossBarTitle = plugin.getConfig().getString("bossbar.pve-title",
                plugin.getConfig().getString("bossbar.title", "&6&lCombat &8| &e{time}s"));
        pvpActionBar = plugin.getConfig().getString("actionbar.pvp", "&cPvP: &f{time}s");
        pveActionBar = plugin.getConfig().getString("actionbar.pve", "&eCombat: &f{time}s");
        pvpBossBarColor = enumValue(BarColor.class, plugin.getConfig().getString("bossbar.pvp-color"),
                enumValue(BarColor.class, plugin.getConfig().getString("bossbar.color"), BarColor.RED));
        pveBossBarColor = enumValue(BarColor.class, plugin.getConfig().getString("bossbar.pve-color"), BarColor.YELLOW);
        warningBossBarColor = enumValue(BarColor.class, plugin.getConfig().getString("bossbar.warning-color"), BarColor.RED);
        bossBarStyle = enumValue(BarStyle.class, plugin.getConfig().getString("bossbar.style"), BarStyle.SEGMENTED_10);

        for (CombatTag tag : tags.values()) {
            tag.lastRemainingSeconds = -1L;
            tag.lastPvp = false;
            tag.lastColor = null;
        }
    }

    public void tag(Player player, boolean pvp) {
        if (player == null || !player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + (durationSeconds * 1000L);
        CombatTag tag = tags.computeIfAbsent(player.getUniqueId(), uuid -> new CombatTag(createBossBar()));
        tag.expiresAt = expiresAt;
        if (pvp) {
            tag.pvpExpiresAt = expiresAt;
        }

        addPlayer(tag, player);
        tag.bar.setVisible(true);
        updateBar(player, tag, now);

        if (pvp && blockElytraInPvp && player.isGliding()) {
            player.setGliding(false);
            player.sendMessage(plugin.message("elytra-blocked"));
        }
    }

    public boolean isTagged(Player player) {
        CombatTag tag = tags.get(player.getUniqueId());
        return tag != null && tag.expiresAt > System.currentTimeMillis();
    }

    public boolean isPvpTagged(Player player) {
        CombatTag tag = tags.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        return tag != null && tag.expiresAt > now && tag.pvpExpiresAt > now;
    }

    public boolean isBlockElytraInPvp() {
        return blockElytraInPvp;
    }

    public boolean isBlockCommandsInPvp() {
        return blockCommandsInPvp;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void clear(Player player) {
        CombatTag tag = tags.remove(player.getUniqueId());
        if (tag != null) {
            tag.bar.removeAll();
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, CombatTag>> iterator = tags.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, CombatTag> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            CombatTag tag = entry.getValue();

            if (player == null || !player.isOnline() || tag.expiresAt <= now) {
                tag.bar.removeAll();
                iterator.remove();
                continue;
            }

            updateBar(player, tag, now);
        }
    }

    private void updateBar(Player player, CombatTag tag, long now) {
        long remainingMillis = Math.max(0L, tag.expiresAt - now);
        long remainingSeconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        double progress = Math.max(0.0D, Math.min(1.0D, remainingMillis / (durationSeconds * 1000.0D)));

        boolean pvp = tag.pvpExpiresAt > now;
        BarColor color = remainingSeconds <= 5L ? warningBossBarColor : (pvp ? pvpBossBarColor : pveBossBarColor);

        addPlayer(tag, player);
        tag.bar.setProgress(progress);
        if (tag.lastColor != color) {
            tag.bar.setColor(color);
            tag.lastColor = color;
        }
        if (tag.lastStyle != bossBarStyle) {
            tag.bar.setStyle(bossBarStyle);
            tag.lastStyle = bossBarStyle;
        }
        if (tag.lastRemainingSeconds != remainingSeconds || tag.lastPvp != pvp) {
            tag.bar.setTitle(plugin.color(format(pvp ? pvpBossBarTitle : pveBossBarTitle, remainingSeconds)));
            tag.lastRemainingSeconds = remainingSeconds;
            tag.lastPvp = pvp;
        }

        if (actionBarEnabled) {
            sendActionBar(player, format(pvp ? pvpActionBar : pveActionBar, remainingSeconds));
        }
    }

    private void addPlayer(CombatTag tag, Player player) {
        if (!tag.attached) {
            tag.bar.addPlayer(player);
            tag.attached = true;
        }
    }

    private BossBar createBossBar() {
        return Bukkit.createBossBar("", pveBossBarColor, bossBarStyle);
    }

    private String format(String text, long remainingSeconds) {
        return text.replace("{time}", Long.toString(remainingSeconds));
    }

    private void sendActionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(plugin.color(text)));
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static final class CombatTag {
        private final BossBar bar;
        private long expiresAt;
        private long pvpExpiresAt;
        private boolean attached;
        private long lastRemainingSeconds = -1L;
        private boolean lastPvp;
        private BarColor lastColor;
        private BarStyle lastStyle;

        private CombatTag(BossBar bar) {
            this.bar = bar;
        }
    }
}
