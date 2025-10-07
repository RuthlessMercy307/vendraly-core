package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Gestiona el sistema de experiencia (XP), nivel, y puntos de atributo (Stat Points) custom.
 * Opera directamente sobre el objeto RPGStats del jugador para mantener la coherencia del modelo.
 */
public class XPManager {

    private final Main plugin;
    private final StatManager statManager;

    // Constantes para la nivelación (configurables en config.yml)
    private final int BASE_XP;
    private final double XP_MULTIPLIER;
    private final int STAT_POINTS_PER_LEVEL;

    public XPManager(Main plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;

        // Valores cargados desde config.yml con defaults
        this.BASE_XP = plugin.getConfig().getInt("xp.base", 200);
        this.XP_MULTIPLIER = plugin.getConfig().getDouble("xp.multiplier", 1.15);
        this.STAT_POINTS_PER_LEVEL = plugin.getConfig().getInt("xp.statPointsPerLevel", 1);

        plugin.getLogger().info(ChatColor.AQUA + "[VendralyCore] XPManager inicializado. Curva XP: "
                + BASE_XP + " * " + XP_MULTIPLIER);
    }

    /* ---------------------------
       Métodos de Fachada (API Externa)
       --------------------------- */

    /**
     * Fachada para compatibilidad. Añade experiencia.
     */
    public void addExp(Player player, long amount) {
        addXP(player, amount);
    }

    /**
     * Fachada para ScoreboardManager que usa el nombre 'getUnspentStatPoints'.
     */
    public int getUnspentStatPoints(UUID uuid) {
        RPGStats stats = statManager.getStats(uuid);
        if (stats == null) {
            plugin.getLogger().log(Level.WARNING,
                    "Intentando obtener puntos sin gastar para jugador no cargado: " + uuid);
            return 0;
        }
        return stats.getUnspentPoints();
    }

    /* ---------------------------
       Lógica de Nivelación
       --------------------------- */

    /**
     * Calcula la cantidad total de XP requerida para alcanzar el siguiente nivel.
     */
    public long getXPForNextLevel(int level) {
        if (level < 1) return BASE_XP;
        return (long) (BASE_XP * Math.pow(XP_MULTIPLIER, level - 1));
    }

    /**
     * Añade experiencia custom al jugador y gestiona la nivelación.
     */
    public void addXP(Player player, long amount) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();

        RPGStats stats = statManager.getStats(playerId);
        if (stats == null) return;

        long currentXP = stats.getTotalExp();
        int level = stats.getLevel();
        long xpForNextLevel = getXPForNextLevel(level);

        long newXP = currentXP + amount;
        boolean leveledUp = false;

        // Procesar múltiples niveles si aplica
        while (newXP >= xpForNextLevel) {
            newXP -= xpForNextLevel;

            int oldLevel = level;
            level++;
            leveledUp = true;
            xpForNextLevel = getXPForNextLevel(level);

            // Recompensa de nivel
            stats.addUnspentPoints(STAT_POINTS_PER_LEVEL);

            // Mensajes y efectos
            player.sendMessage(ChatColor.BOLD + "" + ChatColor.BLUE + "¡FELICIDADES! "
                    + ChatColor.GREEN + "Has alcanzado el Nivel "
                    + ChatColor.YELLOW + level + ChatColor.GREEN + ".");
            player.sendMessage(ChatColor.GREEN + "Has recibido "
                    + ChatColor.BLUE + STAT_POINTS_PER_LEVEL
                    + " Puntos de Atributo " + ChatColor.GREEN
                    + "para gastar con /atributos.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Lanzar evento custom con nivel anterior y nuevo nivel
            Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, oldLevel, level));
        }

        // Guardar nuevos valores
        stats.setTotalExperience(newXP);
        if (leveledUp) {
            stats.setLevel(level);
            statManager.getAttributeApplier().recalculateStats(player);
        }

        // Actualizar scoreboard SIEMPRE
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateScoreboard(player);
        }

        // Actualizar la barra de XP vanilla
        updateVanillaXPBar(player);

        // Guardar en segundo plano
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            var playerData = plugin.getAuthManager().getPlayerData(playerId);
            if (playerData != null) {
                plugin.getUserDataManager().savePlayerData(playerData);
            } else {
                plugin.getLogger().warning("No se encontró PlayerData para " + player.getName()
                        + " al guardar XP.");
            }
        });
    }

    /**
     * Sincroniza la barra de XP vanilla con el sistema RPG.
     */
    public void updateVanillaXPBar(Player player) {
        if (player == null || !player.isOnline()) return;

        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        int level = stats.getLevel();
        long currentXP = stats.getTotalExp();
        long requiredXP = getXPForNextLevel(level);

        player.setLevel(level);

        float progress = 0.0f;
        if (requiredXP > 0) {
            progress = (float) currentXP / requiredXP;
        }

        if (progress < 0.0f) progress = 0.0f;
        if (progress >= 1.0f) progress = 0.999f; // evitar trigger vanilla

        player.setExp(progress);
    }
}
