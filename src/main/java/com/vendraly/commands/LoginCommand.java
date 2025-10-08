package com.vendraly.commands;

import com.vendraly.core.auth.AuthManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /login.
 */
public class LoginCommand implements CommandExecutorHolder {

    private final AuthManager authManager;

    public LoginCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("Uso: /login <contraseña>", NamedTextColor.YELLOW));
            return true;
        }
        authManager.login(player, args[0]);
        return true;
    }

    @Override
    public String getCommandName() {
        return "login";
    }
}
