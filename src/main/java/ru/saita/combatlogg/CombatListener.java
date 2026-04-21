package ru.saita.combatlogg;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class CombatListener implements Listener {
    private final CombatManager combatManager;

    public CombatListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attackingPlayer = responsiblePlayer(event.getDamager());
        Player victimPlayer = realPlayer(event.getEntity());

        if (attackingPlayer != null && victimPlayer != null && !attackingPlayer.equals(victimPlayer)) {
            combatManager.tag(attackingPlayer, true);
            combatManager.tag(victimPlayer, true);
            return;
        }

        if (attackingPlayer != null && isMob(event.getEntity())) {
            combatManager.tag(attackingPlayer, false);
            return;
        }

        if (victimPlayer != null && isMob(sourceEntity(event.getDamager()))) {
            combatManager.tag(victimPlayer, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player)) {
            return;
        }

        combatManager.clear(player);
        player.setHealth(0.0D);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        combatManager.clear(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (combatManager.isBlockElytraInPvp() && combatManager.isPvpTagged(player)) {
            event.setCancelled(true);
            player.setGliding(false);
            player.sendMessage(CombatLogPlugin.getPlugin(CombatLogPlugin.class).message("elytra-blocked"));
        }
    }

    private Player responsiblePlayer(Entity damager) {
        Entity source = sourceEntity(damager);
        Player player = realPlayer(source);
        if (player != null) {
            return player;
        }

        if (source instanceof Tameable) {
            Tameable tameable = (Tameable) source;
            if (tameable.getOwner() instanceof Player) {
                return realPlayer((Player) tameable.getOwner());
            }
        }

        return null;
    }

    private Entity sourceEntity(Entity damager) {
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Entity) {
                return (Entity) source;
            }
        }
        return damager;
    }

    private Player realPlayer(Entity entity) {
        if (!(entity instanceof Player) || entity.hasMetadata("NPC")) {
            return null;
        }
        return (Player) entity;
    }

    private boolean isMob(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof ArmorStand);
    }
}
