package com.vendraly.commands;

import com.vendraly.core.economy.CashManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Permite transferir efectivo entre jugadores.
 */
public class PayCommand implements CommandExecutorHolder {

    private final CashManager cashManager;

    public PayCommand(CashManager cashManager) {
        this.cashManager = cashManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /pay <jugador> <cantidad>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.text("Cantidad inválida", NamedTextColor.RED));
            return true;
        }
        if (cashManager.transferCash(player, target, amount)) {
            player.sendMessage(Component.text("Transferiste " + amount + " monedas a " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text(player.getName() + " te envió " + amount + " monedas", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No puedes transferir esa cantidad", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public String getCommandName() {
        return "pay";
    }
}
