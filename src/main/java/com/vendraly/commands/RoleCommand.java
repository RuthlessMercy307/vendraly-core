package com.vendraly.commands;

import com.vendraly.core.auth.AuthManager;
import com.vendraly.core.roles.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Permite cambiar roles de jugadores.
 */
public class RoleCommand implements CommandExecutorHolder {

    private final AuthManager authManager;

    public RoleCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vendraly.roles")) {
            sender.sendMessage(Component.text("Sin permisos", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /role <jugador> <rol>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED));
            return true;
        }
        try {
            Role role = Role.valueOf(args[1].toUpperCase());
            authManager.setPlayerRole(target, role);
            sender.sendMessage(Component.text("Rol actualizado", NamedTextColor.GREEN));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Rol inválido", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public String getCommandName() {
        return "role";
    }
}
