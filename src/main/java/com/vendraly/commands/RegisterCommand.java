package com.vendraly.commands;

import com.vendraly.core.auth.AuthManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /register.
 */
public class RegisterCommand implements CommandExecutorHolder {

    private final AuthManager authManager;

    public RegisterCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /register <contraseña> <confirmación>", NamedTextColor.YELLOW));
            return true;
        }
        if (!args[0].equals(args[1])) {
            player.sendMessage(Component.text("Las contraseñas no coinciden.", NamedTextColor.RED));
            return true;
        }
        authManager.register(player, args[0]);
        return true;
    }

    @Override
    public String getCommandName() {
        return "register";
    }
}
