package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class StaminaRegenTask extends BukkitRunnable {

    private final Main plugin;
    private final StaminaBossBarManager bossBarManager;

    // ticks por ejecución (ej: 20 ticks = 1s)
    private final int intervalTicks;

    public StaminaRegenTask(Main plugin, StaminaBossBarManager bossBarManager, int intervalTicks) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            RPGStats stats = plugin.getStatManager().getStats(player.getUniqueId());
            if (stats == null) continue;

            // Si el jugador no está stuneado y no tiene cooldown activo
            if (!stats.isStunned() && !plugin.getStatManager().isInStaminaCooldown(player.getUniqueId())) {
                double regenPerTick = stats.getStaminaRegenPerSecond() * (intervalTicks / 20.0);
                stats.restoreStamina(regenPerTick);
            }

            // Actualizar bossbar si está disponible
            if (bossBarManager != null) {
                bossBarManager.updateStaminaBossBar(player, stats);
            }
        }
    }
}
