package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor del sistema de Dodge/Esquivar usando la tecla Shift (Sneak).
 */
public class ParryManager implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> isInvulnerable = new HashMap<>();

    // Configuración desde config.yml
    private final int cooldownSeconds;
    private final long invulnTicks;
    private final boolean enableEffects;

    public ParryManager(Main plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();

        this.cooldownSeconds = config.getInt("parry.cooldown", 3);
        this.invulnTicks = config.getLong("parry.invulnerability", 16L);
        this.enableEffects = config.getBoolean("parry.enable-effects", true);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!event.isSneaking()) return;

        if (isInvulnerable.getOrDefault(playerId, false)) return;

        if (cooldowns.containsKey(playerId) && cooldowns.get(playerId) > System.currentTimeMillis()) {
            long timeLeft = (cooldowns.get(playerId) - System.currentTimeMillis()) / 1000;
            player.sendActionBar(Component.text("Esquivar en Cooldown (" + (timeLeft + 1) + "s)")
                    .color(NamedTextColor.RED));
            return;
        }

        activateDodge(player);
    }

    private void activateDodge(Player player) {
        UUID playerId = player.getUniqueId();

        long cooldownEndTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.put(playerId, cooldownEndTime);

        isInvulnerable.put(playerId, true);
        player.sendActionBar(Component.text("|| ESQUIVAR ACTIVADO ||").color(NamedTextColor.YELLOW));

        if (enableEffects) {
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.3, 0.5, 0.3, 0.01);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 6, 0.3, 0.5, 0.3, 0.01);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.4f);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isInvulnerable.put(playerId, false);
                player.sendActionBar(Component.text("Esquivar terminado. Próximo uso: " + cooldownSeconds + "s.")
                        .color(NamedTextColor.GRAY));
            }
        }.runTaskLater(plugin, invulnTicks);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID playerId = player.getUniqueId();

        if (isInvulnerable.getOrDefault(playerId, false)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("¡ESQUIVAR! Daño bloqueado.")
                    .color(NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.remove(playerId);
        isInvulnerable.remove(playerId);
    }
}
