package com.vendraly.core.scoreboard;

import com.vendraly.VendralyCore;
import com.vendraly.core.economy.CashManager;
import com.vendraly.core.economy.EconomyManager;
import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stats.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona el scoreboard lateral y actualizaciones periódicas.
 */
public class ScoreboardManager {

    private final VendralyCore plugin;
    private final StatManager statManager;
    private final CashManager cashManager;
    private final EconomyManager economyManager;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(VendralyCore plugin, StatManager statManager, CashManager cashManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.cashManager = cashManager;
        this.economyManager = economyManager;
    }

    public void show(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("vendraly", "dummy", Component.text("Vendraly"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        boards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        update(player);
    }

    public void remove(UUID uuid) {
        boards.remove(uuid);
    }

    public void updateAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            update(online);
        }
    }

    public void update(Player player) {
        Scoreboard scoreboard = boards.get(player.getUniqueId());
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        objective.getScore("Nivel: " + statManager.getLevel(player)).setScore(9);
        objective.getScore("XP: " + statManager.getExperience(player)).setScore(8);
        objective.getScore("Salud: " + player.getHealth()).setScore(7);
        objective.getScore("Stamina: " + (int) player.getFoodLevel()).setScore(6);
        double strength = plugin.getUserDataManager().getOrCreate(player.getUniqueId(), player.getName()).getStats().getStat(StatType.STRENGTH);
        objective.getScore("Fuerza: " + (int) strength).setScore(5);
        objective.getScore("Cash: " + cashManager.getBalance(player.getUniqueId())).setScore(4);
        objective.getScore("Banco: " + economyManager.getBalance(player)).setScore(3);
        objective.getScore("Rol: " + player.getName()).setScore(2);
    }

    public void shutdown() {
        boards.clear();
    }
}
