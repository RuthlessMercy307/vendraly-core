package com.vendraly.core.scoreboard;

import com.vendraly.core.Main;
import com.vendraly.core.roles.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maneja los nombres de jugadores con prefijos de rol arriba de la cabeza y en la tablist
 */
public class NameTagManager {

    private final Main plugin;
    private final Map<UUID, String> playerTeams = new HashMap<>();

    public NameTagManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Actualiza el tag del jugador (arriba de la cabeza y tablist)
     */
    public void updatePlayerTag(Player player) {
        Role role = plugin.getAuthManager().getPlayerRole(player);
        String teamName = getTeamName(role);
        String prefix = String.valueOf(role.getFormattedPrefix());

        // Usar el scoreboard principal para que todos vean los mismos tags
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainScoreboard.getTeam(teamName);

        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName);
            // Esto configura el prefijo para el nombre arriba de la cabeza (Name Tag)
            team.setPrefix(prefix);
            // Opcional: configurar color del equipo
            team.setColor(role.getColor());
        }

        // Remover de equipo anterior
        String previousTeam = playerTeams.get(player.getUniqueId());
        if (previousTeam != null && !previousTeam.equals(teamName)) {
            Team oldTeam = mainScoreboard.getTeam(previousTeam);
            if (oldTeam != null) {
                oldTeam.removeEntry(player.getName());
            }
        }

        // Agregar al nuevo equipo
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName()); // Aplica el prefijo al Name Tag
        }

        playerTeams.put(player.getUniqueId(), teamName);

        // 1. Configurar el nombre en la Tablist (CON prefijo)
        player.setPlayerListName(prefix + player.getName());

        // 2. SOLUCIÓN CHAT: Forzar que el Display Name (usado en el chat) sea SÓLO el nombre
        // del jugador. Esto anula la filtración del prefijo del Team al chat.
        player.setDisplayName(player.getName());
    }

    /**
     * Remueve el tag del jugador
     */
    public void removePlayerTag(Player player) {
        String teamName = playerTeams.remove(player.getUniqueId());
        if (teamName != null) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
        // Restaurar nombre original en tablist y display name (siempre importante al salir)
        player.setPlayerListName(null);
        player.setDisplayName(player.getName());
    }

    private String getTeamName(Role role) {
        // Crear nombres de equipo basados en el orden de prioridad del rol
        int priority = getRolePriority(role);
        return String.format("%02d-%s", priority, role.name());
    }

    private int getRolePriority(Role role) {
        // Orden de prioridad (mayor número = mayor prioridad)
        switch (role) {
            case OWNER: return 10;
            case DEVELOPMENT: return 9;
            case MODERADOR: return 8;
            case HELPER: return 7;
            case MEDIA: return 6;
            case VIP: return 5;
            case PLAYER: return 1;
            default: return 1;
        }
    }

    /**
     * Actualiza tags de todos los jugadores online
     */
    public void updateAllTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTag(player);
        }
    }
}
