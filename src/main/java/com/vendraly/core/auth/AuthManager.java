package com.vendraly.core.auth;

import com.vendraly.VendralyCore;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.roles.Role;
import com.vendraly.core.roles.RoleManager;
import com.vendraly.core.security.AuthUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla el ciclo de autenticación de los jugadores, gestionando registros,
 * logins y estados temporales durante la sesión.
 */
public class AuthManager {

    private final VendralyCore plugin;
    private final UserDataManager userDataManager;
    private final RoleManager roleManager;
    private final Map<UUID, Boolean> sessionState = new ConcurrentHashMap<>();

    public AuthManager(VendralyCore plugin, UserDataManager userDataManager, RoleManager roleManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
        this.roleManager = roleManager;
    }

    public boolean isAuthenticated(UUID uuid) {
        return sessionState.getOrDefault(uuid, false);
    }

    public void register(Player player, String password) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        if (data.getPasswordHash() != null && !data.getPasswordHash().isEmpty()) {
            player.sendMessage(Component.text("Ya tienes una cuenta registrada.", NamedTextColor.RED));
            return;
        }
        data.setPasswordHash(AuthUtil.hashPassword(password));
        data.setAuthenticated(true);
        sessionState.put(player.getUniqueId(), true);
        userDataManager.save(data);
        roleManager.applyRole(player, data.getRole());
        player.sendMessage(Component.text("Registro completado. ¡Bienvenido!", NamedTextColor.GREEN));
    }

    public void login(Player player, String password) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        if (data.getPasswordHash() == null || data.getPasswordHash().isEmpty()) {
            player.sendMessage(Component.text("Debes registrarte primero.", NamedTextColor.RED));
            return;
        }
        if (!AuthUtil.checkPassword(password, data.getPasswordHash())) {
            player.sendMessage(Component.text("Contraseña incorrecta.", NamedTextColor.RED));
            return;
        }
        data.setAuthenticated(true);
        sessionState.put(player.getUniqueId(), true);
        roleManager.applyRole(player, data.getRole());
        userDataManager.save(data);
        player.sendMessage(Component.text("Inicio de sesión exitoso.", NamedTextColor.GREEN));
    }

    public void logout(Player player) {
        sessionState.remove(player.getUniqueId());
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        data.resetAuth();
        userDataManager.save(data);
    }

    public void setPlayerRole(Player player, Role role) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        data.setRole(role);
        userDataManager.save(data);
        roleManager.applyRole(player, role);
    }

    public void handleJoin(Player player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        boolean alreadyAuthenticated = data.isAuthenticated();
        if (alreadyAuthenticated) {
            sessionState.put(player.getUniqueId(), true);
            roleManager.applyRole(player, data.getRole());
            player.sendMessage(Component.text("Autenticación recordada. Bienvenido de nuevo.", NamedTextColor.GREEN));
        } else {
            sessionState.put(player.getUniqueId(), false);
            player.sendMessage(Component.text("Usa /login <contraseña> o /register <contraseña> <confirmación>", NamedTextColor.YELLOW));
        }
    }

    public void handleQuit(Player player) {
        sessionState.remove(player.getUniqueId());
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        data.setAuthenticated(false);
        userDataManager.save(data);
    }

    public Map<UUID, Boolean> getSessionState() {
        return sessionState;
    }
}
