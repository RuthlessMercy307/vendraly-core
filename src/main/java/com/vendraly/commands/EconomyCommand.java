package com.vendraly.commands;

import com.vendraly.core.Main;
import com.vendraly.core.economy.CashManager;
import com.vendraly.core.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyCommand implements CommandExecutor {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final CashManager cashManager;

    public EconomyCommand(Main plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.cashManager = plugin.getCashManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        plugin.getLogger().info("Comando /eco ejecutado por " + sender.getName() + " con args: " + String.join(" ", args));

        if (!sender.hasPermission("vendraly.economy.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        // Debug
        plugin.getLogger().info("Subcomando: " + subCommand + ", Target: " + targetName);

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        plugin.getLogger().info("UUID del target: " + targetUUID);

        // 1. Manejar balance
        if (subCommand.equals("balance")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "Uso: /eco balance <jugador> <cash|bank>");
                return true;
            }

            String accountType = args[2].toLowerCase();
            plugin.getLogger().info("Tipo de cuenta: " + accountType);

            if (!plugin.getUserDataManager().playerFileExists(targetUUID)) {
                sender.sendMessage(ChatColor.RED + "El jugador '" + targetName + "' no está registrado en la base de datos.");
                return true;
            }

            if (accountType.equals("bank")) {
                economyManager.getBalance(targetUUID).thenAccept(balance -> {
                    sender.sendMessage(ChatColor.GREEN + "Saldo bancario de " + targetName + ": " + ChatColor.GOLD + String.format("$%.2f", balance));
                }).exceptionally(throwable -> {
                    sender.sendMessage(ChatColor.RED + "Error al obtener el saldo bancario.");
                    plugin.getLogger().warning("Error en getBalance: " + throwable.getMessage());
                    return null;
                });
            } else if (accountType.equals("cash")) {
                cashManager.getCash(targetUUID).thenAccept(balance -> {
                    sender.sendMessage(ChatColor.GREEN + "Efectivo robable de " + targetName + ": " + ChatColor.GOLD + String.format("$%.2f", balance));
                }).exceptionally(throwable -> {
                    sender.sendMessage(ChatColor.RED + "Error al obtener el efectivo.");
                    plugin.getLogger().warning("Error en getCash: " + throwable.getMessage());
                    return null;
                });
            } else {
                sender.sendMessage(ChatColor.RED + "Tipo de cuenta inválido. Usa 'cash' o 'bank'.");
            }
            return true;
        }

        // 2. Manejar give/take/set
        if (args.length != 4) {
            sendUsage(sender);
            return true;
        }

        if (!plugin.getUserDataManager().playerFileExists(targetUUID)) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + targetName + "' no está registrado en la base de datos.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            plugin.getLogger().info("Cantidad: " + amount);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Cantidad inválida. Debe ser un número.");
            return true;
        }

        String accountType = args[3].toLowerCase();
        plugin.getLogger().info("Tipo de cuenta para modificación: " + accountType);

        if (!accountType.equals("bank") && !accountType.equals("cash")) {
            sender.sendMessage(ChatColor.RED + "Tipo de cuenta inválido. Usa 'cash' o 'bank'.");
            return true;
        }

        // Procesar subcomandos
        if (subCommand.equals("give")) {
            plugin.getLogger().info("Ejecutando give...");
            modifyBalance(sender, targetName, targetUUID, amount, accountType, "añadido", true);
        } else if (subCommand.equals("take")) {
            plugin.getLogger().info("Ejecutando take...");
            modifyBalance(sender, targetName, targetUUID, -amount, accountType, "retirado", true);
        } else if (subCommand.equals("set")) {
            plugin.getLogger().info("Ejecutando set...");
            if (accountType.equals("bank")) {
                economyManager.getBalance(targetUUID).thenAccept(currentBalance -> {
                    double difference = amount - currentBalance;
                    plugin.getLogger().info("Set bank - Actual: " + currentBalance + ", Nuevo: " + amount + ", Diferencia: " + difference);
                    modifyBalance(sender, targetName, targetUUID, difference, accountType, "establecido", false);
                }).exceptionally(throwable -> {
                    sender.sendMessage(ChatColor.RED + "Error al obtener el saldo actual.");
                    return null;
                });
            } else {
                cashManager.getCash(targetUUID).thenAccept(currentCash -> {
                    double difference = amount - currentCash;
                    plugin.getLogger().info("Set cash - Actual: " + currentCash + ", Nuevo: " + amount + ", Diferencia: " + difference);
                    modifyBalance(sender, targetName, targetUUID, difference, accountType, "establecido", false);
                }).exceptionally(throwable -> {
                    sender.sendMessage(ChatColor.RED + "Error al obtener el efectivo actual.");
                    return null;
                });
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Subcomando inválido. Usa 'give', 'take', 'set' o 'balance'.");
        }

        return true;
    }

    private void modifyBalance(CommandSender sender, String targetName, UUID targetUUID, double amount, String type, String action, boolean isRelative) {
        plugin.getLogger().info("Modificando balance - Tipo: " + type + ", Cantidad: " + amount + ", Acción: " + action);

        if (type.equals("bank")) {
            economyManager.modifyBalance(targetUUID, amount).thenAccept(success -> {
                plugin.getLogger().info("Resultado modifyBalance bank: " + success);
                notifySender(sender, targetName, targetUUID, amount, type, action, success, isRelative);
            }).exceptionally(throwable -> {
                plugin.getLogger().warning("Error en modifyBalance bank: " + throwable.getMessage());
                sender.sendMessage(ChatColor.RED + "Error interno al modificar el saldo bancario.");
                return null;
            });
        } else if (type.equals("cash")) {
            cashManager.modifyBalance(targetUUID, amount).thenAccept(success -> {
                plugin.getLogger().info("Resultado modifyBalance cash: " + success);
                notifySender(sender, targetName, targetUUID, amount, type, action, success, isRelative);
            }).exceptionally(throwable -> {
                plugin.getLogger().warning("Error en modifyBalance cash: " + throwable.getMessage());
                sender.sendMessage(ChatColor.RED + "Error interno al modificar el efectivo.");
                return null;
            });
        }
    }

    private void notifySender(CommandSender sender, String targetName, UUID targetUUID, double amount, String type, String action, boolean success, boolean isRelative) {
        if (success) {
            String amountText = isRelative ?
                    String.format("$%.2f", Math.abs(amount)) :
                    String.format("$%.2f", amount + (amount < 0 ? -amount * 2 : 0));

            sender.sendMessage(ChatColor.GREEN + String.format("Se ha %s %s al saldo %s de %s.", action, amountText, type, targetName));

            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                plugin.getScoreboardManager().updatePlayerCashCache(targetUUID);
                plugin.getScoreboardManager().updatePlayerBoard(targetPlayer);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Error al modificar el saldo. Operación fallida o saldo insuficiente.");
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso incorrecto.");
        sender.sendMessage(ChatColor.YELLOW + "/eco balance <jugador> <cash|bank>");
        sender.sendMessage(ChatColor.YELLOW + "/eco <give|take|set> <jugador> <cantidad> <cash|bank>");
        sender.sendMessage(ChatColor.GRAY + "Ejemplos:");
        sender.sendMessage(ChatColor.GRAY + "/eco balance Steve cash");
        sender.sendMessage(ChatColor.GRAY + "/eco give Steve 100 bank");
        sender.sendMessage(ChatColor.GRAY + "/eco set Steve 500 cash");
    }
}