package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;

/**
 * Listener que maneja vida, estamina y sprint, conectando eventos de Bukkit
 * con los sistemas RPG (atributos, staminaBossBar, cooldowns).
 */
public class StatListeners implements Listener {

    private final Main plugin;
    private final StatManager statManager;
    private final AttributeApplier attributeApplier;
    private final StaminaBossBarManager bossBarManager;
    private final Map<UUID, Long> staminaCooldowns;

    public StatListeners(Main plugin, StatManager statManager,
                         AttributeApplier attributeApplier,
                         StaminaBossBarManager bossBarManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.attributeApplier = attributeApplier;
        this.bossBarManager = bossBarManager;
        this.staminaCooldowns = statManager.getStaminaCooldowns();
    }

    /** Evita que el jugador inicie sprint si su estamina es demasiado baja. */
    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (!event.isSprinting()) return;

        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) {
            event.setCancelled(true);
            return;
        }

        if (stats.getCurrentStamina() < 5.0) {
            event.setCancelled(true);
            player.sendActionBar(
                    net.kyori.adventure.text.Component.text("⚡ Estamina insuficiente para correr",
                            net.kyori.adventure.text.format.NamedTextColor.GRAY)
            );
        }
    }

    /** Limpia bossbar, efectos y cooldowns al morir. */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (bossBarManager != null) {
            bossBarManager.removeStaminaBossBar(player);
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        staminaCooldowns.remove(player.getUniqueId());
    }

    /** Restaura atributos, vida RPG y estamina al respawnear. */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RPGStats stats = statManager.getStats(player.getUniqueId());
            if (stats == null) return;

            // Re-aplicar atributos (solo afectan a cálculos RPG)
            attributeApplier.applyPlayerAttributes(player, stats);

            // Resetear vida RPG
            stats.setCurrentHealth(stats.getMaxHealth());

            // Resetear estamina
            stats.setCurrentStamina(stats.getMaxStamina());

            if (bossBarManager != null) {
                bossBarManager.updateStaminaBossBar(player, stats);
            }
        }, 1L);
    }
}
