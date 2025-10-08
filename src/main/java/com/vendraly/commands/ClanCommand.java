package com.vendraly.commands;

import com.vendraly.core.clans.Clan;
import com.vendraly.core.clans.ClanManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Comando para gestionar clanes.
 */
public class ClanCommand implements CommandExecutorHolder {

    private final ClanManager clanManager;

    public ClanCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Uso: /clan create|invite|join|leave|war", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Uso: /clan create <id> <nombre>", NamedTextColor.YELLOW));
                    return true;
                }
                clanManager.createClan(player, args[1], args[2]);
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Uso: /clan invite <jugador>", NamedTextColor.YELLOW));
                    return true;
                }
                Player invited = Bukkit.getPlayer(args[1]);
                if (invited == null) {
                    player.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED));
                    return true;
                }
                if (clanManager.invite(player, invited)) {
                    player.sendMessage(Component.text("Invitación enviada", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("No perteneces a un clan", NamedTextColor.RED));
                }
            }
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Uso: /clan join <id>", NamedTextColor.YELLOW));
                    return true;
                }
                if (clanManager.join(player, args[1])) {
                    player.sendMessage(Component.text("Te uniste al clan", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("No tienes invitación", NamedTextColor.RED));
                }
            }
            case "leave" -> clanManager.leave(player);
            case "war" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Uso: /clan war <id>", NamedTextColor.YELLOW));
                    return true;
                }
                Clan own = clanManager.getClanByPlayer(player.getUniqueId());
                if (own == null) {
                    player.sendMessage(Component.text("No perteneces a un clan", NamedTextColor.RED));
                    return true;
                }
                clanManager.declareWar(own.getId(), args[1]);
            }
            default -> player.sendMessage(Component.text("Subcomando inválido", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public String getCommandName() {
        return "clan";
    }
}
