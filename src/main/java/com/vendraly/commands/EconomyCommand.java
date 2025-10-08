package com.vendraly.commands;

import com.vendraly.core.economy.CashManager;
import com.vendraly.core.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Comando administrativo para gestionar saldos.
 */
public class EconomyCommand implements CommandExecutorHolder {

    private final EconomyManager economyManager;
    private final CashManager cashManager;

    public EconomyCommand(EconomyManager economyManager, CashManager cashManager) {
        this.economyManager = economyManager;
        this.cashManager = cashManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vendraly.economy.admin")) {
            sender.sendMessage(Component.text("Sin permisos.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(Component.text("Uso: /eco <give|take|set> <jugador> <cantidad> <cash|bank>", NamedTextColor.YELLOW));
            return true;
        }
        String action = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("Cantidad inválida", NamedTextColor.RED));
            return true;
        }
        boolean cash = args[3].equalsIgnoreCase("cash");
        switch (action.toLowerCase()) {
            case "give" -> {
                if (cash) {
                    cashManager.modify(target.getUniqueId(), amount);
                } else {
                    economyManager.deposit(target.getUniqueId(), amount);
                }
            }
            case "take" -> {
                if (cash) {
                    cashManager.modify(target.getUniqueId(), -amount);
                } else {
                    economyManager.withdraw(target.getUniqueId(), amount);
                }
            }
            case "set" -> {
                if (cash) {
                    cashManager.modify(target.getUniqueId(), amount - cashManager.getBalance(target.getUniqueId()));
                } else {
                    economyManager.setBalance(target.getUniqueId(), amount);
                }
            }
            default -> {
                sender.sendMessage(Component.text("Acción inválida.", NamedTextColor.RED));
                return true;
            }
        }
        sender.sendMessage(Component.text("Economía actualizada", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public String getCommandName() {
        return "eco";
    }
}
