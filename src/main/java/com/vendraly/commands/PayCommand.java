package com.vendraly.commands;

import com.vendraly.core.Main;
import com.vendraly.core.economy.CashManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Permite a los jugadores pagar dinero en efectivo a otro jugador usando CashManager.
 */
public class PayCommand implements CommandExecutor {

    private final Main plugin;
    private final CashManager cashManager;
    private static final double MIN_PAYMENT = 0.01;

    public PayCommand(Main plugin) {
        this.plugin = plugin;
        this.cashManager = plugin.getCashManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }

        final Player payer = (Player) sender;
        final UUID payerUUID = payer.getUniqueId();

        // 1. Bloqueo de autenticación (Usamos AuthManager)
        if (!plugin.getAuthManager().isAuthenticated(payerUUID)) {
            payer.sendMessage(ChatColor.RED + "Debes iniciar sesión para usar el comando /pay.");
            return true;
        }

        // 2. Revisar sintaxis
        if (args.length != 2) {
            payer.sendMessage(ChatColor.RED + "Uso: /pay <jugador> <cantidad>");
            return true;
        }

        final String targetName = args[0];
        final double amount;

        try {
            amount = Math.round(Double.parseDouble(args[1]) * 100.0) / 100.0; // Redondear a dos decimales

            if (amount < MIN_PAYMENT) {
                payer.sendMessage(ChatColor.RED + "La cantidad mínima a pagar es de $" + MIN_PAYMENT);
                return true;
            }
            if (amount <= 0) {
                payer.sendMessage(ChatColor.RED + "La cantidad debe ser positiva.");
                return true;
            }
        } catch (NumberFormatException e) {
            payer.sendMessage(ChatColor.RED + "Cantidad inválida. Debe ser un número.");
            return true;
        }

        // 3. Verificar jugador objetivo (debe estar online y ser diferente)
        final Player receiver = Bukkit.getPlayerExact(targetName);
        if (receiver == null || !receiver.isOnline()) {
            payer.sendMessage(ChatColor.RED + "El jugador '" + targetName + "' no está online.");
            return true;
        }
        if (payer.equals(receiver)) {
            payer.sendMessage(ChatColor.RED + "No puedes pagarte a ti mismo.");
            return true;
        }
        final UUID receiverUUID = receiver.getUniqueId();

        // 4. Bloquear pago a un jugador no autenticado (Usamos AuthManager)
        if (!plugin.getAuthManager().isAuthenticated(receiverUUID)) {
            payer.sendMessage(ChatColor.RED + "No puedes pagar a un jugador que no ha iniciado sesión.");
            return true;
        }

        payer.sendMessage(ChatColor.YELLOW + "Verificando saldo y procesando pago...");

        // 5. Transferencia Asíncrona completa (evitamos el .join())
        cashManager.getBalance(payerUUID)
                .thenCompose(totalCash -> {
                    if (totalCash < amount) {
                        // Si no tiene suficiente, se lanza una excepción que se captura abajo
                        throw new IllegalArgumentException(String.format("No tienes suficiente dinero en efectivo. Tienes $%.2f, necesitas $%.2f.", totalCash, amount));
                    }
                    // Si tiene suficiente, procede a la transferencia
                    return cashManager.transferCash(payerUUID, receiverUUID, amount);
                })
                .thenAccept(success -> {
                    // Ejecutar en el hilo principal para interactuar con Bukkit API (mensajes y scoreboard)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            String formattedAmount = String.format("$%.2f", amount);
                            payer.sendMessage(ChatColor.GREEN + "Has pagado " + ChatColor.GOLD + formattedAmount + ChatColor.GREEN + " a " + receiver.getName());

                            // Re-chequeamos el estado del receptor antes de enviar el mensaje
                            Player currentReceiver = Bukkit.getPlayer(receiverUUID);
                            if (currentReceiver != null && currentReceiver.isOnline()) {
                                currentReceiver.sendMessage(ChatColor.GREEN + "Has recibido " + ChatColor.GOLD + formattedAmount + ChatColor.GREEN + " de " + payer.getName());
                                plugin.getScoreboardManager().updatePlayerBoard(currentReceiver);
                            }
                            plugin.getScoreboardManager().updatePlayerBoard(payer);
                        } else {
                            payer.sendMessage(ChatColor.RED + "Error interno al procesar el pago. Operación de transferencia fallida.");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    // Captura el error de saldo insuficiente o cualquier otro error asíncrono
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (throwable.getCause() instanceof IllegalArgumentException) {
                            // Error de saldo insuficiente
                            payer.sendMessage(ChatColor.RED + throwable.getCause().getMessage());
                        } else {
                            // Error de base de datos o consulta
                            payer.sendMessage(ChatColor.RED + "Error al consultar tu saldo o procesar el pago. Inténtalo de nuevo.");
                            plugin.getLogger().warning("Error en el comando /pay: " + throwable.getMessage());
                        }
                    });
                    return null;
                });

        return true;
    }
}