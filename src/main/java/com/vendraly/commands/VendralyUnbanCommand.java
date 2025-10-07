package com.vendraly.commands;

import com.vendraly.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

/**
 * Comando para desbanear jugadores usando YAML.
 * Uso: /vunban <jugador>
 */
public class VendralyUnbanCommand implements CommandExecutor {

    private final Main plugin;
    private static final String UNBAN_PERMISSION = "vendralycore.mod.unban";

    public VendralyUnbanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(UNBAN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para desbanear jugadores.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /" + label + " <jugador>");
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "No se pudo resolver el nombre del jugador '" + targetName + "'.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        // Verificar si el jugador existe en el sistema YAML
        if (!plugin.getUserDataManager().playerFileExists(targetUUID)) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + targetName + "' no está registrado en el sistema.");
            return true;
        }

        // Verificar si está baneado
        if (!plugin.getUserDataManager().getPlayerConfig(targetUUID).getBoolean("security.is-banned", false)) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + targetName + "' no está baneado.");
            return true;
        }

        // Aplicar el desban en YAML
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.is-banned", false);
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.ban-reason", "");
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.banned-by", "");
        plugin.getUserDataManager().savePlayerConfig(targetUUID);

        // Broadcast y mensaje de confirmación
        Bukkit.broadcastMessage(ChatColor.GREEN + "[VendralyBan] " + ChatColor.YELLOW + targetName +
                ChatColor.GREEN + " ha sido desbaneado por " + sender.getName() + ".");
        sender.sendMessage(ChatColor.GREEN + "Jugador " + targetName + " desbaneado con éxito. Ahora puede volver a conectarse.");

        return true;
    }
}