package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.auth.AuthManager;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.utils.NMSHealthUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gestiona el bloqueo de jugadores no autenticados y la inicialización de datos al login.
 */
public class PlayerListener implements Listener {

    private final Main plugin;
    private final AuthManager authManager;

    // Lista de jugadores que NO han hecho /login o /register (bloqueados).
    private final Set<UUID> unauthenticatedPlayers = new HashSet<>();

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
    }

    // =========================================================
    //   EVENTOS DE CONEXIÓN / DESCONEXIÓN
    // =========================================================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        event.joinMessage(null); // Ocultamos mensaje vanilla de entrada

        if (authManager.isAuthenticated(uuid)) {
            // Si ya está logueado (datos en memoria), lo desbloqueamos de una
            onLoginSuccess(player);
            return;
        }

        // Bloqueado hasta que haga login/register
        unauthenticatedPlayers.add(uuid);

        // Enviamos mensaje tras pequeño retraso para evitar que se pierda
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(Component.text("--------------------------------------------------")
                    .color(NamedTextColor.DARK_GRAY));
            if (authManager.isRegistered(uuid)) {
                player.sendMessage(Component.text("Bienvenido " + player.getName() + "! Usa ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text("/login <contraseña>").color(NamedTextColor.YELLOW)));
            } else {
                player.sendMessage(Component.text("Bienvenido, nuevo usuario! Usa ")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("/register <contraseña> <confirmar>").color(NamedTextColor.RED)));
            }
            player.sendMessage(Component.text("--------------------------------------------------")
                    .color(NamedTextColor.DARK_GRAY));
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removePlayerBoard(player);
        }

        unauthenticatedPlayers.remove(uuid);

        // Guardar datos de logout de forma asíncrona
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            authManager.logoutPlayer(uuid);
        });

        event.quitMessage(null);
    }

    // =========================================================
    //   BLOQUEOS PARA NO AUTENTICADOS
    // =========================================================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!unauthenticatedPlayers.contains(uuid)) return;

        // Cancelar solo si cambió de bloque (evita spam innecesario)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            event.setCancelled(true);
            player.sendActionBar(Component.text("Debes autenticarte para moverte. Usa /register o /login.")
                    .color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!unauthenticatedPlayers.contains(uuid)) return;

        String command = event.getMessage().toLowerCase();
        if (!command.startsWith("/register") && !command.startsWith("/login")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Solo puedes usar /register o /login.")
                    .color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (unauthenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    //   LOGIN EXITOSO
    // =========================================================
    /**
     * Llamado por LoginCommand y RegisterCommand tras autenticación exitosa.
     */
    public void onLoginSuccess(Player player) {
        UUID uuid = player.getUniqueId();
        RPGStats stats = plugin.getStatManager().getStats(uuid);

        // 1. Desbloquear jugador
        unauthenticatedPlayers.remove(uuid);

        // 2. Cargar rol
        authManager.loadRoleFromData(uuid);

        // 3. Aplicar atributos RPG
        if (stats != null) {
            plugin.getStatManager().applyPlayerAttributes(player, stats);
        } else {
            plugin.getLogger().log(Level.WARNING, "No se encontraron RPGStats para " + player.getName() + " al hacer login.");
            return;
        }

        // 4. Sincronizar XP
        if (plugin.getXPManager() != null) {
            plugin.getXPManager().updateVanillaXPBar(player);
        }

        // 5. Ocultar vida vanilla (con retraso de 1 tick)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int rpgMaxHealth = (int) plugin.getStatManager().getRpgMaxHealth(uuid);
            NMSHealthUtil.hideVanillaHealth(player, rpgMaxHealth);

            // 6. Cargar scoreboard después de aplicar stats
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updatePlayerBoard(player);
            }
        }, 1L);

        // 7. Mensaje de éxito con rol
        player.sendMessage(Component.text("¡Autenticación completada! Tu rol es ")
                .color(NamedTextColor.GREEN)
                .append(authManager.getPlayerRole(player).getFormattedPrefix()));
    }

    // =========================================================
    //   UTILIDAD
    // =========================================================
    public boolean isUnauthenticated(Player player) {
        return unauthenticatedPlayers.contains(player.getUniqueId());
    }
}
