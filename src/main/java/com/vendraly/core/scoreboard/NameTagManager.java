package com.vendraly.core.scoreboard;

import com.vendraly.core.Main;
import com.vendraly.core.roles.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Maneja los nombres de jugadores con prefijos de rol
 * arriba de la cabeza (NameTag) y en la Tablist.
 */
public class NameTagManager {

    private final Main plugin;
    private final Map<UUID, String> playerTeams = new HashMap<>();

    public NameTagManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Actualiza el tag del jugador (arriba de la cabeza y en tablist).
     */
    public void updatePlayerTag(Player player) {
        Role role = plugin.getAuthManager().getPlayerRole(player);
        String teamName = getTeamName(role);

        // Convertir el Component del prefijo a String legacy (§)
        String prefix = LegacyComponentSerializer.legacySection()
                .serialize(role.getFormattedPrefix());

        // Scoreboard global
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainScoreboard.getTeam(teamName);

        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName);
            // Prefijo arriba de la cabeza
            team.setPrefix(prefix);
            // Color opcional en la tablist
            team.setColor(role.getColor());
        } else {
            // Actualizar prefix por si cambió
            team.setPrefix(prefix);
            team.setColor(role.getColor());
        }

        // Quitar de equipo anterior si cambió
        String previousTeam = playerTeams.get(player.getUniqueId());
        if (previousTeam != null && !previousTeam.equals(teamName)) {
            Team oldTeam = mainScoreboard.getTeam(previousTeam);
            if (oldTeam != null) {
                oldTeam.removeEntry(player.getName());
            }
        }

        // Añadir al nuevo equipo
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        playerTeams.put(player.getUniqueId(), teamName);

        // Mostrar prefijo también en la TAB
        player.setPlayerListName(prefix + player.getName());

        // En el chat solo el nombre limpio (sin prefijo doble)
        player.setDisplayName(player.getName());
    }

    /**
     * Remueve el tag del jugador.
     */
    public void removePlayerTag(Player player) {
        String teamName = playerTeams.remove(player.getUniqueId());
        if (teamName != null) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
        // Restaurar nombres originales
        player.setPlayerListName(null);
        player.setDisplayName(player.getName());
    }

    private String getTeamName(Role role) {
        int priority = getRolePriority(role);
        return String.format("%02d-%s", priority, role.name());
    }

    private int getRolePriority(Role role) {
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
     * Refresca tags de todos los jugadores online.
     */
    public void updateAllTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTag(player);
        }
    }
}
