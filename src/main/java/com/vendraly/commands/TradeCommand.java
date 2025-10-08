package com.vendraly.commands;

import com.vendraly.core.trade.TradeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para gestionar trades.
 */
public class TradeCommand implements CommandExecutorHolder {

    private final TradeManager tradeManager;

    public TradeCommand(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Uso: /trade <player> | /trade accept <player> | /trade money <cantidad>", NamedTextColor.YELLOW));
            return true;
        }
        if (args[0].equalsIgnoreCase("accept") && args.length >= 2) {
            Player requester = Bukkit.getPlayer(args[1]);
            if (requester == null) {
                player.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED));
                return true;
            }
            if (!tradeManager.accept(player, requester)) {
                player.sendMessage(Component.text("No hay solicitud pendiente", NamedTextColor.RED));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("money") && args.length >= 2) {
            try {
                double amount = Double.parseDouble(args[1]);
                if (tradeManager.offerMoney(player, amount)) {
                    player.sendMessage(Component.text("Oferta monetaria actualizada", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("No puedes ofertar esa cantidad", NamedTextColor.RED));
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(Component.text("Cantidad inválida", NamedTextColor.RED));
            }
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED));
            return true;
        }
        tradeManager.requestTrade(player, target);
        return true;
    }

    @Override
    public String getCommandName() {
        return "trade";
    }
}
