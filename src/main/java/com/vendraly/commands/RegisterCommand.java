package com.vendraly.commands;

import com.vendraly.core.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RegisterCommand implements CommandExecutor {

    private final Main plugin;

    public RegisterCommand(Main plugin) {
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
        String playerName = player.getName();

        // 1. Bloqueo si ya está autenticado
        if (plugin.getAuthManager().isAuthenticated(playerUUID)) {
            player.sendMessage(ChatColor.YELLOW + "Ya has iniciado sesión. No necesitas registrarte de nuevo.");
            return true;
        }

        // 2. Validar Argumentos
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Uso: /register <contraseña> <confirmar_contraseña>");
            player.sendMessage(ChatColor.GRAY + "Nota: Las contraseñas se pasan como dos palabras separadas.");
            return true;
        }

        String password = args[0];
        String repeatPassword = args[1];

        // 3. Comprobar que ambas contraseñas coincidan
        if (!password.equals(repeatPassword)) {
            player.sendMessage(ChatColor.RED + "¡Error! Las contraseñas no coinciden. Inténtalo de nuevo.");
            return true;
        }

        // Validación de longitud mínima (Mejoramos la longitud a 6 para mayor seguridad)
        if (password.length() < 6) {
            player.sendMessage(ChatColor.RED + "La contraseña debe tener al menos 6 caracteres.");
            return true;
        }

        // 4. Comprobar si ya está registrado (Chequeo redundante pero seguro antes del proceso)
        if (plugin.getAuthManager().isRegistered(playerUUID)) {
            player.sendMessage(ChatColor.RED + "Ya estás registrado. Usa /login <contraseña>.");
            return true;
        }

        // 5. Intentar el registro
        plugin.getLogger().info(playerName + " | Intentando registro...");
        // registerPlayer también debe llamar a loginPlayer/onLoginSuccess internamente
        if (plugin.getAuthManager().registerPlayer(playerUUID, password)) {

            // El AuthManager ahora maneja la autenticación y el envío del mensaje de bienvenida/desbloqueo.
            player.sendMessage(ChatColor.GREEN + "Registro completado con éxito. ¡Bienvenido!");

        } else {
            player.sendMessage(ChatColor.RED + "Error interno al registrarte. Contacta a un administrador.");
            plugin.getLogger().severe(playerName + " | REGISTRO FALLIDO: Fallo en AuthManager.registerPlayer.");
        }

        return true;
    }
}