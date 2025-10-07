package com.vendraly.commands;

import com.vendraly.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Comando para banear jugadores usando YAML.
 * Uso: /vban <jugador> [razón]
 */
public class VendralyBanCommand implements CommandExecutor {

    private final Main plugin;
    private static final String BAN_PERMISSION = "vendralycore.mod.ban";

    public VendralyBanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(BAN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para banear jugadores.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /" + label + " <jugador> [razón]");
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(ChatColor.RED + "Jugador '" + targetName + "' no encontrado o nunca ha jugado.");
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No se especificó una razón.";

        // Proteger a los administradores
        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            if (plugin.getAuthManager().getPlayerRole(onlineTarget).isOp()) {
                sender.sendMessage(ChatColor.RED + "No puedes banear a un administrador (Owner/Development).");
                return true;
            }
        }

        // Aplicar el ban en YAML
        UUID targetUUID = target.getUniqueId();
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.is-banned", true);
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.ban-reason", reason);
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.banned-by", sender.getName());
        plugin.getUserDataManager().getPlayerConfig(targetUUID).set("security.ban-time", System.currentTimeMillis());
        plugin.getUserDataManager().savePlayerConfig(targetUUID);

        // Si está online, kickearlo
        if (target.isOnline()) {
            Player player = (Player) target;
            String kickMessage = ChatColor.RED + "¡Has sido BANEADO!\n\n" +
                    ChatColor.YELLOW + "Razón: " + ChatColor.WHITE + reason + "\n" +
                    ChatColor.YELLOW + "Baneado por: " + ChatColor.WHITE + sender.getName();
            player.kickPlayer(kickMessage);
        }

        // Broadcast y mensaje de confirmación
        Bukkit.broadcastMessage(ChatColor.RED + "[VendralyBan] " + ChatColor.YELLOW + targetName +
                ChatColor.RED + " ha sido baneado por " + sender.getName() + " (" + reason + ")");
        sender.sendMessage(ChatColor.GREEN + "Jugador " + targetName + " baneado con éxito.");

        return true;
    }
}