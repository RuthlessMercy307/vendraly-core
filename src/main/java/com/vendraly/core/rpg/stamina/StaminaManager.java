package com.vendraly.core.rpg.stamina;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maneja la barra de stamina de los jugadores.
 */
public class StaminaManager {

    private final Map<UUID, Double> stamina = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public StaminaManager() {
    }

    public void initialize(Player player) {
        stamina.put(player.getUniqueId(), 100.0);
        BossBar bar = Bukkit.createBossBar(Component.text("Stamina"), BarColor.GREEN, BarStyle.SEGMENTED_10);
        bar.addPlayer(player);
        bossBars.put(player.getUniqueId(), bar);
    }

    public boolean consume(Player player, double amount) {
        double current = stamina.getOrDefault(player.getUniqueId(), 0.0);
        if (current < amount) {
            player.sendActionBar(Component.text("Sin stamina", NamedTextColor.RED));
            return false;
        }
        stamina.put(player.getUniqueId(), current - amount);
        return true;
    }

    public void regenerate(Player player, double amount) {
        double current = stamina.getOrDefault(player.getUniqueId(), 0.0);
        stamina.put(player.getUniqueId(), Math.min(100.0, current + amount));
    }

    public void tick() {
        for (UUID uuid : stamina.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                regenerate(player, 2.0);
                BossBar bar = bossBars.get(uuid);
                if (bar != null) {
                    bar.setProgress(Math.max(0.0, stamina.get(uuid) / 100.0));
                }
            }
        }
    }

    public void remove(UUID uuid) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        stamina.remove(uuid);
    }

    public void shutdown() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        stamina.clear();
    }
}
