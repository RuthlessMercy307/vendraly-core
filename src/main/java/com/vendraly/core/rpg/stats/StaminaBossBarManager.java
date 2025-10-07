package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class StaminaBossBarManager {

    private final Main plugin;
    private final Map<UUID, BossBar> staminaBars;

    // Umbrales configurables
    private static final float LOW_THRESHOLD = 0.25f;
    private static final float MID_THRESHOLD = 0.5f;

    public StaminaBossBarManager(Main plugin, Map<UUID, BossBar> staminaBars) {
        this.plugin = plugin;
        this.staminaBars = staminaBars;
    }

    /** Crear y mostrar la bossbar de estamina por primera vez */
    public void addStaminaBossBar(Player player) {
        if (staminaBars.containsKey(player.getUniqueId())) return;

        BossBar bar = BossBar.bossBar(
                Component.text("Estamina inicializando...").color(NamedTextColor.GRAY),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );
        player.showBossBar(bar);
        staminaBars.put(player.getUniqueId(), bar);
    }

    /** Actualizar bossbar de estamina (recrea si se perdió) */
    public void updateStaminaBossBar(Player player, RPGStats stats) {
        BossBar bar = staminaBars.get(player.getUniqueId());
        if (bar == null) {
            addStaminaBossBar(player);
            bar = staminaBars.get(player.getUniqueId());
            if (bar == null) {
                plugin.getLogger().warning("No se pudo recrear BossBar de estamina para " + player.getName());
                return;
            }
        }

        double current = Math.min(stats.getCurrentStamina(), stats.getMaxStamina());
        current = Math.max(0, current);
        float progress = (float) (current / stats.getMaxStamina());

        // Determinar color y texto
        BossBar.Color barColor;
        NamedTextColor textColor;
        if (progress < LOW_THRESHOLD) {
            barColor = BossBar.Color.RED;
            textColor = NamedTextColor.RED;
        } else if (progress < MID_THRESHOLD) {
            barColor = BossBar.Color.YELLOW;
            textColor = NamedTextColor.YELLOW;
        } else {
            barColor = BossBar.Color.GREEN;
            textColor = NamedTextColor.GREEN;
        }

        // Actualizar propiedades
        bar.progress(progress);
        bar.color(barColor);
        bar.overlay(BossBar.Overlay.NOTCHED_20); // opcional, estilo segmentado
        bar.name(Component.text(String.format("⚡ Estamina: %.0f / %.0f", current, stats.getMaxStamina()))
                .color(textColor));
    }

    /** Eliminar bossbar del jugador */
    public void removeStaminaBossBar(Player player) {
        BossBar bar = staminaBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }
}
