package com.vendraly.commands;

import com.vendraly.core.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LoginCommand implements CommandExecutor {

    private final Main plugin;

    public LoginCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Uso: /login <contraseña>");
            return true;
        }

        String password = args[0];

        // 1. Bloqueo si ya está autenticado (Usamos AuthManager, que es la fuente de verdad)
        if (plugin.getAuthManager().isAuthenticated(playerUUID)) {
            player.sendMessage(ChatColor.YELLOW + "Ya has iniciado sesión.");
            return true;
        }

        // El LoginListener bloquea la mayoría de las interacciones si el jugador está en la lista de no autenticados,
        // pero este comando debe seguir el flujo estricto del AuthManager.

        // 2. Comprobar si está registrado
        if (!plugin.getAuthManager().isRegistered(playerUUID)) {
            player.sendMessage(ChatColor.RED + "No estás registrado. Usa /register <contraseña> <confirmar>.");
            return true;
        }

        // 3. Intentar el login (AuthManager maneja la verificación del hash, la autenticación y el llamado a onLoginSuccess)
        if (plugin.getAuthManager().loginPlayer(playerUUID, password)) {
            // El mensaje de éxito lo debe enviar onLoginSuccess
        } else {
            player.sendMessage(ChatColor.RED + "Contraseña incorrecta. Inténtalo de nuevo.");
        }

        return true;
    }
}