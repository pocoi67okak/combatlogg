package ru.saita.combatlogg;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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
    private String bossBarTitle;
    private BarColor bossBarColor;
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
        bossBarTitle = plugin.getConfig().getString("bossbar.title", "&cCombat: &f{time}s");
        bossBarColor = enumValue(BarColor.class, plugin.getConfig().getString("bossbar.color"), BarColor.RED);
        bossBarStyle = enumValue(BarStyle.class, plugin.getConfig().getString("bossbar.style"), BarStyle.SEGMENTED_10);

        for (CombatTag tag : tags.values()) {
            tag.bar.setColor(bossBarColor);
            tag.bar.setStyle(bossBarStyle);
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

        addPlayer(tag.bar, player);
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

        addPlayer(tag.bar, player);
        tag.bar.setProgress(progress);
        tag.bar.setTitle(plugin.color(bossBarTitle.replace("{time}", Long.toString(remainingSeconds))));
    }

    private void addPlayer(BossBar bar, Player player) {
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private BossBar createBossBar() {
        return Bukkit.createBossBar("", bossBarColor, bossBarStyle);
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

        private CombatTag(BossBar bar) {
            this.bar = bar;
        }
    }
}
