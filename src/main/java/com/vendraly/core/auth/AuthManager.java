package com.vendraly.core.auth;

import com.vendraly.core.Main;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.roles.Role;
import com.vendraly.core.security.AuthUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona autenticación y roles usando YAML (o sistema de almacenamiento).
 * Es el punto central para el estado de autenticación y roles de jugadores ONLINE.
 */
public class AuthManager {

    private final Main plugin;
    private final UserDataManager dataManager;

    // Almacenamiento en memoria de permisos (PermissionsAttachment)
    private final Map<UUID, PermissionAttachment> playerPermissions = new HashMap<>();

    // CACHÉ: Almacenamiento en memoria de PlayerData para jugadores ONLINE
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public AuthManager(Main plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getUserDataManager();

        // Cargar datos de jugadores online al inicio (por ejemplo en /reload)
        Bukkit.getOnlinePlayers().forEach(p -> {
            PlayerData data = dataManager.loadPlayerData(p.getUniqueId(), p.getName());
            playerDataCache.put(p.getUniqueId(), data);
            loadRoleFromData(p.getUniqueId());
        });
    }

    // =============================
    // MANEJO DE CACHE
    // =============================

    public PlayerData loadPlayerToCache(Player player) {
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId(), player.getName());
        playerDataCache.put(player.getUniqueId(), data);
        loadRoleFromData(player.getUniqueId());
        return data;
    }

    public void unloadPlayerFromCache(UUID uuid) {
        logoutPlayer(uuid); // asegura guardado
        playerDataCache.remove(uuid);
        removeRolePermissions(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    // =============================
    // REGISTRO Y LOGIN
    // =============================

    public boolean isRegistered(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) return data.isRegistered();
        return dataManager.isRegistered(uuid);
    }

    @Deprecated
    public boolean isRegistered(String username) {
        Player player = Bukkit.getPlayerExact(username);
        if (player != null) {
            return isRegistered(player.getUniqueId());
        }
        // Mejor: delegar a UserDataManager para obtener UUID
        UUID uuid = dataManager.getUUIDFromUsername(username).join();
        return uuid != null && isRegistered(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        return data != null && data.isLoggedIn();
    }

    public boolean registerPlayer(UUID uuid, String password) {
        if (isRegistered(uuid)) return false;

        Player player = Bukkit.getPlayer(uuid);
        String username = (player != null) ? player.getName() : "UNKNOWN";

        String hashedPassword = AuthUtil.hashPassword(password);
        if (hashedPassword == null) {
            plugin.getLogger().warning("Error al registrar " + username + ": hash nulo.");
            return false;
        }

        PlayerData data = dataManager.loadPlayerData(uuid, username);
        data.setRegistered(true);
        data.setPasswordHash(hashedPassword);
        data.setLoggedIn(true);
        data.setCurrentRole(Role.PLAYER);

        dataManager.savePlayerData(data);
        playerDataCache.put(uuid, data);

        if (player != null) {
            setPlayerRole(uuid, Role.PLAYER);
            plugin.getPlayerListener().onLoginSuccess(player);
        }

        plugin.getLogger().info("Jugador " + username + " registrado con éxito.");
        return true;
    }

    public boolean loginPlayer(UUID uuid, String password) {
        PlayerData data = getPlayerData(uuid);
        if (data == null || !data.isRegistered()) return false;

        if (!AuthUtil.checkPassword(password, data.getPasswordHash())) {
            data.setFailedAttempts(data.getFailedAttempts() + 1);
            dataManager.savePlayerData(data);
            return false;
        }

        data.setLoggedIn(true);
        data.setFailedAttempts(0);
        dataManager.savePlayerData(data);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getPlayerListener().onLoginSuccess(player);
            plugin.getLogger().info(player.getName() + " ha iniciado sesión con éxito.");
        }

        return true;
    }

    public void logoutPlayer(UUID uuid) {
        removeRolePermissions(uuid);
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setLoggedIn(false);
            dataManager.savePlayerData(data);
        }
    }

    // =============================
    // ROLES
    // =============================

    public Role getPlayerRole(Player player) {
        PlayerData data = getPlayerData(player.getUniqueId());
        return (data != null) ? data.getCurrentRole() : Role.PLAYER;
    }

    public Role loadRoleFromData(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        if (data == null) return Role.PLAYER;

        Role role = data.getCurrentRole();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyRolePermissions(player, role);
        }
        return role;
    }

    public void setPlayerRole(UUID uuid, Role role) {
        if (role == null) role = Role.PLAYER;

        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setCurrentRole(role);
            dataManager.savePlayerData(data);
        } else {
            dataManager.setPlayerRole(uuid, role);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyRolePermissions(player, role);
            player.sendMessage(ChatColor.GREEN + "Tu rol ha sido actualizado a " + role.getFormattedPrefix());
        }
    }

    private void applyRolePermissions(Player player, Role role) {
        removeRolePermissions(player.getUniqueId());

        if (player.isOp()) player.setOp(false);
        if (role.isOp()) {
            player.setOp(true);
            return;
        }

        if (!role.getPermissions().isEmpty()) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            for (String perm : role.getPermissions()) {
                attachment.setPermission(perm, true);
            }
            playerPermissions.put(player.getUniqueId(), attachment);
        }
    }

    private void removeRolePermissions(UUID uuid) {
        PermissionAttachment attachment = playerPermissions.remove(uuid);
        if (attachment != null) {
            attachment.remove();
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            PlayerData data = getPlayerData(uuid);
            if (player.isOp() && (data == null || !data.getCurrentRole().isOp())) {
                player.setOp(false);
            }
        }
    }
}
