package com.vendraly.core.rpg.ability;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.rpg.RPGStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Clase encargada de gestionar la experiencia, niveles y progresión
 * de las habilidades secundarias (TAILORING, BLACKSMITHING, MINING, etc.).
 * Se basa en la información almacenada en RPGStats y los límites definidos en AbilityType.
 */
public class AbilityManager {

    private final Main plugin;

    public AbilityManager(Main plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // ACCESO A DATOS
    // ============================================================

    private RPGStats getStats(UUID uuid) {
        PlayerData data = plugin.getAuthManager().getPlayerData(uuid);
        if (data != null) return data.getRpgStats();

        plugin.getLogger().log(Level.WARNING,
                "[AbilityManager] RPGStats no encontrada para UUID: " + uuid);
        return null;
    }

    private PlayerData getPlayerData(UUID uuid) {
        PlayerData data = plugin.getAuthManager().getPlayerData(uuid);
        if (data == null) {
            plugin.getLogger().log(Level.WARNING,
                    "[AbilityManager] PlayerData no encontrado para UUID: " + uuid);
        }
        return data;
    }

    // ============================================================
    // CONSULTAS DE NIVELES Y EXP
    // ============================================================

    public long getRequiredExpForNextLevel(int currentLevel, AbilityType abilityType) {
        if (currentLevel < 1) return 50;
        if (currentLevel >= abilityType.getMaxLevel()) return Long.MAX_VALUE;

        return (long) (50 * Math.pow(currentLevel + 1, 1.8));
    }

    public boolean canGainExp(Player player, AbilityType abilityType) {
        if (abilityType.usesPoints()) return false;

        RPGStats stats = getStats(player.getUniqueId());
        if (stats == null) return false;

        return getLevel(player.getUniqueId(), abilityType) < abilityType.getMaxLevel();
    }

    public int getLevel(UUID uuid, AbilityType abilityType) {
        RPGStats stats = getStats(uuid);
        return (stats != null) ? stats.getAbilityLevel(abilityType) : 0;
    }

    public int getAbilityPoints(UUID uuid, AbilityType abilityType) {
        RPGStats stats = getStats(uuid);
        return (stats != null) ? stats.getAbilityLevel(abilityType) : 0;
    }

    public boolean setAbilityPoints(UUID uuid, AbilityType abilityType, int points) {
        if (!abilityType.usesPoints()) return false;

        PlayerData data = getPlayerData(uuid);
        if (data == null) return false;

        RPGStats stats = data.getRpgStats();
        int newLevel = Math.min(Math.max(points, 0), abilityType.getMaxLevel());

        stats.setAbilityLevel(abilityType, newLevel);
        plugin.getUserDataManager().savePlayerData(data);

        return true;
    }

    // ============================================================
    // MÉTODOS ADMIN (usados por comandos tipo /rpgexp)
    // ============================================================

    public boolean setAbilityLevelAdmin(UUID playerId, AbilityType type, int amount, String action) {
        PlayerData data = getPlayerData(playerId);
        if (data == null) return false;

        RPGStats stats = data.getRpgStats();
        if (stats == null) return false;

        int currentLevel = stats.getAbilityLevel(type);
        int newLevel = currentLevel;

        switch (action.toLowerCase()) {
            case "set":
                newLevel = amount;
                break;
            case "add":
                newLevel = currentLevel + amount;
                break;
            case "remove":
                newLevel = currentLevel - amount;
                break;
            default:
                return false;
        }

        newLevel = Math.max(0, Math.min(newLevel, type.getMaxLevel()));
        stats.setAbilityLevel(type, newLevel);

        // Reset de EXP si es acción administrativa directa
        stats.setAbilityExp(type, 0);

        plugin.getUserDataManager().savePlayerData(data);
        return true;
    }

    // ============================================================
    // GANAR EXP Y SUBIR NIVELES
    // ============================================================

    public void addExp(Player player, AbilityType abilityType, int amount) {
        if (abilityType.usesPoints() || player == null || amount <= 0) return;

        UUID uuid = player.getUniqueId();
        PlayerData data = getPlayerData(uuid);
        if (data == null) return;

        RPGStats stats = data.getRpgStats();

        if (getLevel(uuid, abilityType) >= abilityType.getMaxLevel()) return;

        long newTotalExp = stats.getAbilityExp(abilityType) + amount;
        stats.setAbilityExp(abilityType, newTotalExp);

        checkSkillLevelUp(player, stats, abilityType);

        plugin.getUserDataManager().savePlayerData(data);
    }

    private void checkSkillLevelUp(Player player, RPGStats stats, AbilityType abilityType) {
        int currentLevel = stats.getAbilityLevel(abilityType);
        long currentExp = stats.getAbilityExp(abilityType);
        int maxLevel = abilityType.getMaxLevel();

        boolean leveledUp = false;
        long requiredExpForLastLevelUp = 0;

        while (currentLevel < maxLevel) {
            long requiredExp = getRequiredExpForNextLevel(currentLevel, abilityType);

            if (currentExp < requiredExp) break;

            requiredExpForLastLevelUp = requiredExp;
            currentLevel++;
            stats.setAbilityLevel(abilityType, currentLevel);
            leveledUp = true;

            // Mensaje de subida
            player.sendMessage(Component.text("[RPG] ¡Tu habilidad de ")
                    .append(Component.text(abilityType.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" ha subido a nivel " + currentLevel + "!"))
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD));
        }

        if (leveledUp) {
            stats.setAbilityExp(abilityType, currentExp - requiredExpForLastLevelUp);

            if (plugin.getScoreboardManager() != null) {
                // TODO: Actualizar scoreboard con habilidades secundarias
            }
        }
    }
}
