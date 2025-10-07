package com.vendraly.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.combat.DirectionalAttackManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    private final StatManager statManager;
    private final Main plugin;

    public PlayerJoinListener(StatManager statManager) {
        this.statManager = statManager;
        this.plugin = statManager.getPlugin();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                // 1. Re-sincronizar visuales (vida, estamina, etc.)
                statManager.updatePlayerVisuals(player);

                // 2. Inicializar Scoreboard RPG
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().initPlayerScoreboard(player);
                }

                // 3. Indicador de ataque direccional
                DirectionalAttackManager manager = plugin.getDirectionalAttackManager();
                if (manager != null) {
                    manager.createIndicator(player);
                }

                // 4. BossBar de estamina (CRÍTICO)
                if (plugin.getStaminaBossBarManager() != null) {
                    plugin.getStaminaBossBarManager().addStaminaBossBar(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                // Re-sincronizar visuals (vida/estamina)
                statManager.updatePlayerVisuals(player);

                // Indicador de ataque direccional
                DirectionalAttackManager manager = plugin.getDirectionalAttackManager();
                if (manager != null) {
                    manager.createIndicator(player);
                }

                // BossBar de estamina también en respawn
                if (plugin.getStaminaBossBarManager() != null) {
                    plugin.getStaminaBossBarManager().addStaminaBossBar(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
