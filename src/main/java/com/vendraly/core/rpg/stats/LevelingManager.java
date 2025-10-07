package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class LevelingManager {

    private final Main plugin;
    private final StatManager statManager;

    public LevelingManager(Main plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    /**
     * Añade experiencia al jugador: puede ser main (nivel global) o una skill pasiva.
     */
    public void addExp(Player player, String skillName, long amount) {
        if (player == null || amount <= 0) return;
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;

        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        String key = skillName.toUpperCase();

        boolean leveledUp = false;

        if (key.equals("MAIN")) {
            leveledUp = stats.addExp(amount); // usa el núcleo de RPGStats
            if (leveledUp) {
                player.sendMessage(Component.text("§a[RPG] ¡Has subido al Nivel " + stats.getLevel() + "!")
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(" Ahora tienes puntos de atributo para gastar.").color(NamedTextColor.YELLOW)));

                // Aplicar atributos de inmediato
                statManager.getAttributeApplier().applyPlayerAttributes(player, stats);

                // Actualizar exp bar vanilla
                updatePlayerExpBar(player, stats);

                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().notifyLevelChange(player);
                }
            } else {
                // Solo actualizar barra si no subió
                updatePlayerExpBar(player, stats);
            }
        } else {
            try {
                stats.addSkillExp(key, amount);
                int newLevel = stats.getSkillLevel(key);
                if (newLevel > 0 && amount > 0) {
                    String message = getSkillLevelUpMessage(key, newLevel);
                    player.sendMessage(Component.text("§b[RPG] ¡Tu habilidad de " + key + " ha subido al Nivel " + newLevel + "!")
                            .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                    player.sendMessage(Component.text("§7  " + message).color(NamedTextColor.GRAY));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error al actualizar la EXP de la habilidad " + key, e);
            }
        }

        statManager.savePlayerStats(player.getUniqueId());
    }

    private String getSkillLevelUpMessage(String skillKey, int newLevel) {
        return switch (skillKey) {
            case StatManager.BLACKSMITHING -> "Ahora puedes crear armas con mayor durabilidad y estadísticas.";
            case StatManager.TAILORING -> "Mejoras la probabilidad de crear armaduras con bonificaciones.";
            case StatManager.APOTHECARY -> {
                String bonus = String.format("%.1f", newLevel * 0.5);
                yield "Las pociones que crafteas y consumes tienen un " + bonus + "% más de efectividad.";
            }
            default -> "Tu habilidad ha mejorado. ¡Consulta /stats para ver los detalles!";
        };
    }

    /**
     * Actualiza la barra de experiencia vanilla con el progreso del nivel RPG.
     */
    public void updatePlayerExpBar(Player player, RPGStats stats) {
        int currentLevel = stats.getLevel();
        player.setLevel(currentLevel);

        long expNeededForNext = RPGStats.getExpForNextLevel(currentLevel + 1);
        long expForCurrent = RPGStats.getExpForNextLevel(currentLevel);

        long expAtThisLevel = stats.getTotalExp() - expForCurrent;
        long expRange = expNeededForNext - expForCurrent;

        float progress = (expRange > 0) ? (float) expAtThisLevel / (float) expRange : 0f;
        player.setExp(Math.max(0f, Math.min(1f, progress)));
    }
}
