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

        // Cargar datos de jugadores online al inicio
        Bukkit.getOnlinePlayers().forEach(p -> {
            PlayerData data = dataManager.loadPlayerData(p.getUniqueId(), p.getName());
            playerDataCache.put(p.getUniqueId(), data);
            // CORRECCIÓN: Asegurar que los permisos se apliquen al inicio
            loadRoleFromData(p.getUniqueId());
        });
    }

    /**
     * Carga o actualiza el caché de datos de un jugador.
     * Usado por PlayerJoinListener.
     */
    public PlayerData loadPlayerToCache(Player player) {
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId(), player.getName());
        playerDataCache.put(player.getUniqueId(), data);
        loadRoleFromData(player.getUniqueId()); // Aplicar rol al entrar
        return data;
    }

    /**
     * Limpia el caché de datos de un jugador al desconectarse.
     * Usado por PlayerQuitListener.
     */
    public void unloadPlayerFromCache(UUID uuid) {
        logoutPlayer(uuid); // Asegura que el estado isLoggedIn se guarde/limpie
        playerDataCache.remove(uuid);
        removeRolePermissions(uuid); // Limpiar permisos de la memoria
    }

    /**
     * Obtiene datos del jugador (con caché).
     * Solo devuelve datos si el jugador está en el caché (ONLINE o recién registrado).
     */
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    /**
     * Comprueba si un jugador está registrado.
     * Si no está en caché (está offline), DELEGA la verificación al UserDataManager.
     */
    public boolean isRegistered(UUID uuid) {
        PlayerData data = getPlayerData(uuid);

        // Jugador online (en caché)
        if (data != null) {
            return data.isRegistered();
        }

        // Jugador offline (delegamos la consulta al data manager, que debe chequear el archivo)
        // CORRECCIÓN INVISIBLE: No usar OfflinePlayer.hasPlayedBefore() para chequear registro.
        return plugin.getUserDataManager().isRegistered(uuid);
    }

    /**
     * **DEPRECADO:** Chequear registro por nombre es inseguro y lento.
     * Este método debería ser eliminado o ser asíncrono.
     * Mantengo la lógica original para evitar errores de compilación, pero advierto su riesgo.
     */
    @Deprecated
    public boolean isRegistered(String username) {
        // La implementación moderna debería usar un Future<UUID> y luego llamar a isRegistered(UUID)
        // Dejamos el chequeo solo si el jugador está ONLINE para la compatibilidad con Bukkit.
        Player onlinePlayer = Bukkit.getPlayerExact(username);
        if (onlinePlayer != null) {
            return isRegistered(onlinePlayer.getUniqueId());
        }

        // Si no está online, se requiere una consulta lenta/insegura o delegación a UserDataManager
        // Asumiendo que UserDataManager tiene un método para resolver UUID por nombre síncrono.
        return plugin.getUserDataManager().isRegistered(UUID.fromString(username));
    }

    /**
     * Comprueba si un jugador está autenticado
     */
    public boolean isAuthenticated(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        // El jugador solo puede estar autenticado si está en nuestro caché (online)
        return data != null && data.isLoggedIn();
    }

    /**
     * Registra un nuevo jugador
     */
    public boolean registerPlayer(UUID uuid, String password) {
        if (isRegistered(uuid)) {
            return false;
        }

        // CORRECCIÓN: Aseguramos obtener el nombre del jugador, que debe estar online en este punto
        Player player = Bukkit.getPlayer(uuid);
        String username = (player != null) ? player.getName() : "UNKNOWN";

        String hashedPassword = AuthUtil.hashPassword(password);
        if (hashedPassword == null) return false;

        // Cargar/Crear datos (usamos loadPlayerData para obtener una instancia persistente)
        PlayerData data = dataManager.loadPlayerData(uuid, username);
        data.setRegistered(true);
        data.setPasswordHash(hashedPassword);
        data.setLoggedIn(true);
        data.setCurrentRole(Role.PLAYER); // Rol por defecto

        dataManager.savePlayerData(data);
        playerDataCache.put(uuid, data); // Asegurar que el caché se actualice

        // Aplicar rol y desbloqueo
        if (player != null) {
            setPlayerRole(uuid, Role.PLAYER); // Aplica permisos inmediatamente
            // CORRECCIÓN: LoginPlayer llama a onLoginSuccess. Aquí lo hacemos directamente.
            plugin.getPlayerListener().onLoginSuccess(player);
        }

        plugin.getLogger().info("Jugador " + username + " (" + uuid + ") registrado.");
        return true;
    }

    /**
     * Intenta loguear un jugador
     */
    public boolean loginPlayer(UUID uuid, String password) {
        PlayerData data = getPlayerData(uuid);

        // Chequeo si el jugador ya está en caché (online) y registrado
        if (data == null || !data.isRegistered()) {
            return false;
        }

        // 1. Verificar contraseña
        if (!AuthUtil.checkPassword(password, data.getPasswordHash())) {
            // Incrementar intentos fallidos
            data.setFailedAttempts(data.getFailedAttempts() + 1);
            dataManager.savePlayerData(data);
            return false;
        }

        // 2. Login exitoso
        data.setLoggedIn(true);
        data.setFailedAttempts(0);
        dataManager.savePlayerData(data);

        // 3. Aplicar rol y DESBLOQUEAR.
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Llama a onLoginSuccess para quitar el bloqueo de movimiento/chat
            plugin.getPlayerListener().onLoginSuccess(player);
            plugin.getLogger().info(player.getName() + " ha iniciado sesión con éxito.");
        }

        return true;
    }

    /**
     * Cierra sesión del jugador (usado al desconectarse o al usar /logout)
     */
    public void logoutPlayer(UUID uuid) {
        removeRolePermissions(uuid);

        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setLoggedIn(false);
            dataManager.savePlayerData(data); // Guardar estado de desconectado
            // NO eliminamos del caché aquí, lo hace unloadPlayerFromCache en el PlayerQuitEvent
        }
    }

    // --- Sistema de Roles ---

    public Role getPlayerRole(Player player) {
        PlayerData data = getPlayerData(player.getUniqueId());
        // CORRECCIÓN: Si la data es nula (error, no debería pasar si está online), por defecto es PLAYER.
        return data != null ? data.getCurrentRole() : Role.PLAYER;
    }

    /**
     * Carga el rol desde los datos YAML y aplica permisos si está online.
     */
    public Role loadRoleFromData(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        if (data == null) return Role.PLAYER;

        Role role = data.getCurrentRole();

        // Aplicar permisos si está online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyRolePermissions(player, role);
        }

        return role;
    }

    /**
     * Establece el rol de un jugador
     */
    public void setPlayerRole(UUID uuid, Role role) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            // Si el jugador está en caché (online), actualizamos su rol
            data.setCurrentRole(role);
            dataManager.savePlayerData(data);
        } else {
            // CORRECCIÓN INVISIBLE: Si el jugador está offline (no en caché), necesitamos actualizar los datos directamente
            // Esto asume que UserDataManager permite actualizar roles sin cargar toda la PlayerData.
            plugin.getUserDataManager().setPlayerRole(uuid, role);
        }

        // Aplicar permisos inmediatamente si está online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyRolePermissions(player, role);
            player.sendMessage(ChatColor.GREEN + "Tu rol ha sido actualizado a " + role.getFormattedPrefix());
        }
    }

    /**
     * Aplica permisos del rol
     */
    private void applyRolePermissions(Player player, Role role) {
        removeRolePermissions(player.getUniqueId());

        // CORRECCIÓN: Quitar OP antes de ponerlo si el rol anterior lo era.
        if (player.isOp()) {
            player.setOp(false);
        }

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

    /**
     * Remueve permisos
     */
    private void removeRolePermissions(UUID uuid) {
        PermissionAttachment attachment = playerPermissions.remove(uuid);
        if (attachment != null) {
            attachment.remove();
        }

        Player player = Bukkit.getPlayer(uuid);
        // CORRECCIÓN: Si el jugador es OP y el rol YA no lo es, quitamos OP.
        // Pero esta lógica se maneja mejor en applyRolePermissions.
        // Aquí, simplemente aseguramos que el estado de OP sea consistente si el attachment se elimina.
        if (player != null) {
            PlayerData data = getPlayerData(uuid);
            if (player.isOp() && (data == null || !data.getCurrentRole().isOp())) {
                player.setOp(false);
            }
        }
    }
}