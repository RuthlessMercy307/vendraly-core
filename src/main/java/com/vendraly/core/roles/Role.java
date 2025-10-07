package com.vendraly.core.roles;

import org.bukkit.ChatColor;
import java.util.Arrays;
import java.util.List;

/**
 * Define los diferentes roles disponibles en el servidor, sus prefijos y permisos.
 */
public enum Role {

    // Roles Administrativos
    OWNER(ChatColor.DARK_RED, "Owner", true), // isOp = true
    DEVELOPMENT(ChatColor.DARK_AQUA, "Development", true), // isOp = true
    MODERADOR(ChatColor.LIGHT_PURPLE, "Moderador", false,
            "vendralycore.mod.ban",
            "vendralycore.mod.unban",
            "bukkit.command.gamemode", // Acceso a creativo/espectador
            "bukkit.command.teleport"),

    // Roles de Soporte
    HELPER(ChatColor.AQUA, "Helper", false,
            "bukkit.command.teleport"), // Solo para ayudar, no creativo/ban

    // Roles de Estatus/VIP
    MEDIA(ChatColor.YELLOW, "Media", false),
    VIP(ChatColor.GREEN, "VIP", false),

    // Rol por Defecto
    PLAYER(ChatColor.GRAY, "Player", false);

    private final ChatColor color;
    private final String prefix;
    private final boolean isOp;
    private final List<String> permissions;

    /**
     * Constructor para roles que no son OP y tienen permisos específicos.
     */
    Role(ChatColor color, String prefix, boolean isOp, String... permissions) {
        this.color = color;
        this.prefix = prefix;
        this.isOp = isOp;
        this.permissions = Arrays.asList(permissions);
    }

    /**
     * Constructor para roles sin permisos específicos (como VIP/MEDIA) o que son OP.
     */
    Role(ChatColor color, String prefix, boolean isOp) {
        this(color, prefix, isOp, new String[0]);
    }

    // Getters

    public ChatColor getColor() {
        return color;
    }

    /**
     * Obtiene el prefijo formateado para mostrar en el chat.
     * Ejemplo: [Owner]
     */
    public String getFormattedPrefix() {
        return color + "[" + prefix + "] " + ChatColor.RESET;
    }

    /**
     * Obtiene los permisos asociados a este rol.
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Devuelve true si el rol debe tener permisos de operador (OP).
     */
    public boolean isOp() {
        return isOp;
    }
}
