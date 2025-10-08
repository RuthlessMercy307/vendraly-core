package com.vendraly.core.roles;

import com.vendraly.VendralyCore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asigna roles y permisos dinámicamente a los jugadores.
 */
public class RoleManager {

    private final VendralyCore plugin;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public RoleManager(VendralyCore plugin) {
        this.plugin = plugin;
    }

    public void applyRole(Player player, Role role) {
        remove(player.getUniqueId());
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String perm : role.getPermissions()) {
            attachment.setPermission(perm, true);
        }
        attachments.put(player.getUniqueId(), attachment);
        Component display = role.getDisplayName(player.getName());
        player.displayName(display);
        player.playerListName(display);
    }

    public void remove(UUID uuid) {
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            attachment.remove();
        }
    }
}
