package com.vendraly.commands;

import com.vendraly.core.Main;
import com.vendraly.core.roles.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Comando para cambiar el rol de un jugador.
 * Uso: /setrole <jugador> <rol>
 */
public class SetRoleCommand implements CommandExecutor {

    private final Main plugin;
    private static final String PERMISSION = "vendralycore.admin.setrole";

    public SetRoleCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Verificar Permisos
        if (!sender.isOp() && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /setrole <jugador> <rol>");
            sender.sendMessage(ChatColor.GRAY + "Roles disponibles: " +
                    Arrays.stream(Role.values())
                            .map(Role::name)
                            .collect(Collectors.joining(", ")));
            return true;
        }

        // 2. Obtener el jugador objetivo
        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(ChatColor.RED + "Jugador '" + targetName + "' no encontrado o nunca ha jugado.");
            return true;
        }

        // 3. Obtener el Rol
        String roleName = args[1].toUpperCase();
        Role newRole;
        try {
            newRole = Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "El rol '" + roleName + "' no es válido.");
            sender.sendMessage(ChatColor.GRAY + "Roles disponibles: " +
                    Arrays.stream(Role.values())
                            .map(Role::name)
                            .collect(Collectors.joining(", ")));
            return true;
        }

        // 4. Verificar si el jugador existe en el sistema YAML
        if (!plugin.getUserDataManager().playerFileExists(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + targetName + "' no está registrado en el sistema.");
            return true;
        }

        // 5. Aplicar el Rol (Asumiendo que setPlayerRole guarda el rol y lo aplica inmediatamente)
        plugin.getAuthManager().setPlayerRole(target.getUniqueId(), newRole);

        // 6. Enviar mensaje de confirmación
        String successMessage = ChatColor.GREEN + "El rol de " + ChatColor.YELLOW + targetName +
                ChatColor.GREEN + " ha sido cambiado a " + newRole.getFormattedPrefix() + ChatColor.GREEN + " exitosamente.";
        sender.sendMessage(successMessage);

        // Notificar al jugador si está online
        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            onlineTarget.sendMessage(ChatColor.GOLD + "Tu rol ha sido actualizado por " + sender.getName() + ".");
            // Actualizar scoreboard y tags
            plugin.getScoreboardManager().updatePlayerBoard(onlineTarget);
            plugin.getScoreboardManager().getNameTagManager().updatePlayerTag(onlineTarget);
        }

        return true;
    }
}