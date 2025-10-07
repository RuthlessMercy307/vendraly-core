package com.vendraly.core.scoreboard;

import com.vendraly.core.Main;
import com.vendraly.core.auth.AuthManager;
import com.vendraly.core.economy.CashManager;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.XPManager;
import com.vendraly.core.roles.Role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Gestiona la Scoreboard lateral (sidebar) de forma robusta y eficiente.
 */
public class ScoreboardManager {

    private final Main plugin;
    private final AuthManager authManager;
    private final CashManager cashManager;
    private final StatManager statManager;
    private final XPManager xpManager;
    private final NameTagManager nameTagManager;
    private int taskId = -1;

    private final Map<UUID, Scoreboard> activeBoards = new ConcurrentHashMap<>();
    private final Map<UUID, Double> cachedCash = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUpdate = new ConcurrentHashMap<>();
    private static final long UPDATE_COOLDOWN = 1000;
    private static final String SIDEBAR_TITLE = ChatColor.GOLD.toString() + ChatColor.BOLD + ":: VENDRALY CORE ::";

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
        this.cashManager = plugin.getCashManager();
        this.statManager = plugin.getStatManager();
        this.nameTagManager = new NameTagManager(plugin);
        this.xpManager = plugin.getXPManager();
    }

    /**
     * Devuelve el color Adventure correspondiente al rol.
     */
    private NamedTextColor getRoleColor(Role role) {
        if (role == null) return NamedTextColor.WHITE;
        return switch (role) {
            case OWNER -> NamedTextColor.RED;
            case MODERADOR -> NamedTextColor.DARK_GREEN;
            case VIP -> NamedTextColor.GOLD;
            case PLAYER -> NamedTextColor.GRAY;
            default -> NamedTextColor.WHITE;
        };
    }

    public void startUpdateTask() {
        if (taskId != -1) {
            plugin.getLogger().warning("Scoreboard task ya está iniciada.");
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                updateAllCashBalancesAsync();
                updateAllBoards();
                nameTagManager.updateAllTags();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error en la tarea de actualización de scoreboard", e);
            }
        }, 20L, 40L).getTaskId();

        plugin.getLogger().info("Scoreboard y NameTags task iniciada correctamente.");
    }

    public void stopUpdateTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            plugin.getLogger().info("Scoreboard task detenida.");
        }
    }

    private void updateAllCashBalancesAsync() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (!authManager.isAuthenticated(uuid)) {
                continue;
            }

            long now = System.currentTimeMillis();
            if (lastUpdate.containsKey(uuid) && (now - lastUpdate.get(uuid)) < UPDATE_COOLDOWN) {
                continue;
            }

            cashManager.getBalance(uuid).thenAccept(cash -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    cachedCash.put(uuid, cash);
                    lastUpdate.put(uuid, now);
                });
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Error al obtener efectivo para " + player.getName(), throwable);
                return null;
            });
        }
    }

    private void updateAllBoards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                updatePlayerBoard(player);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error al actualizar scoreboard para " + player.getName(), e);
            }
        }
    }

    // -------------------------------
    // Métodos de inicialización
    // -------------------------------

    public void updateScoreboard(Player player) {
        updatePlayerBoard(player);
    }

    public void initPlayerScoreboard(Player player) {
        updatePlayerBoard(player);
    }

    public void notifyHealthChange(Player player) {
        updatePlayerBoard(player);
    }

    public void notifyLevelChange(Player player) {
        updatePlayerBoard(player);
    }

    public void notifyDefenseChange(Player player) {
        updatePlayerBoard(player);
    }

    public void updatePlayerBoard(Player player) {
        if (player == null || !player.isOnline()) return;

        if (!authManager.isAuthenticated(player.getUniqueId())) return;

        try {
            Scoreboard board = getOrCreatePlayerBoard(player.getUniqueId());
            if (board == null) return;

            Objective objective = getOrCreateObjective(board);
            if (objective == null) return;

            clearOldScores(board, objective);

            buildScoreboard(player, board, objective);

            if (player.isOnline()) {
                player.setScoreboard(board);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error crítico al actualizar scoreboard para " + player.getName(), e);
        }
    }

    private Scoreboard getOrCreatePlayerBoard(UUID uuid) {
        return activeBoards.computeIfAbsent(uuid, k -> {
            try {
                return Bukkit.getScoreboardManager().getNewScoreboard();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al crear nuevo scoreboard", e);
                return null;
            }
        });
    }

    private Objective getOrCreateObjective(Scoreboard board) {
        Objective objective = board.getObjective("sidebar");
        if (objective == null) {
            objective = board.registerNewObjective("sidebar", "dummy", SIDEBAR_TITLE);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(SIDEBAR_TITLE);
        }
        return objective;
    }

    private void clearOldScores(Scoreboard board, Objective objective) {
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith("line_")) {
                team.unregister();
            }
        }
    }

    private void buildScoreboard(Player player, Scoreboard board, Objective objective) {
        int score = 10;

        UUID uuid = player.getUniqueId();
        RPGStats stats = statManager.getStats(uuid);
        Role role = authManager.getPlayerRole(player);

        int unspentPoints = (stats != null) ? xpManager.getUnspentStatPoints(uuid) : 0;
        double cash = cachedCash.getOrDefault(uuid, 0.0);
        int fame = getPlayerFame(uuid);

        setScoreboardLine(objective, board, ChatColor.AQUA.toString(), score--); // Espacio invisible

        // Rango
        Component rolePrefixComp = (role != null)
                ? Component.text("").color(getRoleColor(role)).append(role.getFormattedPrefix())
                : Component.text("Campesino", NamedTextColor.WHITE);

        String rolePrefix = LegacyComponentSerializer.legacySection().serialize(rolePrefixComp);
        setScoreboardLine(objective, board, ChatColor.AQUA + "Rango: " + rolePrefix, score--);

        // Dinero
        setScoreboardLine(objective, board,
                ChatColor.GREEN + "Dinero: " + ChatColor.YELLOW + "$" + String.format("%,.0f", cash), score--);

        setScoreboardLine(objective, board, ChatColor.WHITE.toString(), score--);

        // Vida
        String healthLine = ChatColor.RED + "♥ Vida: " + ChatColor.WHITE + "Cargando...";
        if (stats != null && statManager.getAttributeApplier() != null) {
            double rpgMaxHealth = stats.getMaxHealth();
            double rpgCurrentHealth = stats.getCurrentHealth();
            String currentHealthText = String.format("%.0f", rpgCurrentHealth);
            String maxHealthText = String.format("%.0f", rpgMaxHealth);

            healthLine = ChatColor.RED + "♥ Vida: " + ChatColor.WHITE + currentHealthText +
                    ChatColor.DARK_GRAY + "/" + ChatColor.RED + maxHealthText;
        }
        setScoreboardLine(objective, board, healthLine, score--);

        // Experiencia
        String expLine = ChatColor.DARK_AQUA + "Nivel: " + ChatColor.WHITE + "Cargando...";
        if (stats != null) {
            long currentExp = stats.getTotalExp();
            int level = stats.getLevel();
            long requiredExp = xpManager.getXPForNextLevel(level);

            expLine = ChatColor.DARK_AQUA + "Nivel " + level + ": " + ChatColor.WHITE +
                    String.format("%,d", currentExp) + ChatColor.GRAY + "/" +
                    ChatColor.WHITE + String.format("%,d", requiredExp);
        }
        setScoreboardLine(objective, board, expLine, score--);

        // Fama
        String fameColor = (fame < 0) ? ChatColor.RED.toString() : ChatColor.WHITE.toString();
        setScoreboardLine(objective, board, ChatColor.GOLD + "Fama: " + fameColor + fame, score--);

        // Party
        String partyStatus = ChatColor.DARK_AQUA + "Party: " + ChatColor.WHITE + "0/3";
        setScoreboardLine(objective, board, partyStatus, score--);

        setScoreboardLine(objective, board, ChatColor.BLACK.toString(), score--);

        // Puntos disponibles
        if (unspentPoints > 0) {
            setScoreboardLine(objective, board, ChatColor.YELLOW.toString() + ChatColor.BOLD +
                    "¡" + ChatColor.RED + unspentPoints + ChatColor.YELLOW + " PUNTOS DISPONIBLES!", score--);
        }
    }

    private int getPlayerFame(UUID uuid) {
        // TODO: Implementar en PlayerData
        return 0;
    }

    private void setScoreboardLine(Objective objective, Scoreboard board, String text, int score) {
        try {
            String entry = getUniqueEntryByScore(score);
            String teamName = "line_" + score;

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }

            team.setPrefix(text);
            objective.getScore(entry).setScore(score);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al establecer línea de scoreboard: " + text, e);
        }
    }

    private String getUniqueEntryByScore(int score) {
        return ChatColor.values()[score].toString() + ChatColor.RESET;
    }

    public void removePlayerBoard(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        try {
            if (activeBoards.containsKey(uuid)) {
                if (player.isOnline()) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
                activeBoards.remove(uuid);
            }
            cachedCash.remove(uuid);
            lastUpdate.remove(uuid);
            nameTagManager.removePlayerTag(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al remover scoreboard de " + player.getName(), e);
        }
    }

    public void updatePlayerCashCache(UUID uuid) {
        try {
            if (!authManager.isAuthenticated(uuid)) return;

            cashManager.getBalance(uuid).thenAccept(cash -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    cachedCash.put(uuid, cash);
                    lastUpdate.put(uuid, System.currentTimeMillis());
                });
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Error al actualizar caché de efectivo para " + uuid, throwable);
                return null;
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al iniciar actualización de caché para " + uuid, e);
        }
    }

    public NameTagManager getNameTagManager() {
        return nameTagManager;
    }

    public void clearAllCache() {
        activeBoards.clear();
        cachedCash.clear();
        lastUpdate.clear();
        plugin.getLogger().info("Scoreboard cache limpiado.");
    }
}
