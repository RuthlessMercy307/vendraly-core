package com.vendraly.commands;

import com.vendraly.core.Main;
import com.vendraly.core.trade.TradeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TradeCommand implements CommandExecutor {

    private final Main plugin;
    private final TradeManager tradeManager;
    private final String AUTH_ERROR = "Debes iniciar sesión para usar el comando /trade.";

    public TradeCommand(Main plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por un jugador.").color(NamedTextColor.RED));
            return true;
        }

        // Bloqueo de autenticación
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(Component.text(AUTH_ERROR).color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args.length > 2) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept" -> {
                if (args.length != 2) {
                    player.sendMessage(Component.text("Uso: /trade accept <jugador>").color(NamedTextColor.YELLOW));
                    return true;
                }
                handleTradeAccept(player, args[1]);
                return true;
            }
            case "cancel" -> {
                handleTradeCancel(player);
                return true;
            }
            // Si es un nombre de jugador
            default -> {
                if (args.length == 1) {
                    handleTradeRequest(player, args[0]);
                } else {
                    sendUsage(player);
                }
                return true;
            }
        }
    }

    /**
     * Maneja la solicitud inicial de trade: /trade <jugador>
     */
    private void handleTradeRequest(Player requester, String targetName) {
        Optional<Player> targetOpt = tradeManager.getTargetPlayer(targetName);

        if (targetOpt.isEmpty()) {
            requester.sendMessage(Component.text("El jugador '").color(NamedTextColor.RED)
                    .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                    .append(Component.text("' no está online o no existe.").color(NamedTextColor.RED)));
            return;
        }

        Player target = targetOpt.get();

        if (requester.equals(target)) {
            requester.sendMessage(Component.text("No puedes tradear contigo mismo.").color(NamedTextColor.RED));
            return;
        }

        // Bloqueo de autenticación para el target
        if (!plugin.getAuthManager().isAuthenticated(target.getUniqueId())) {
            requester.sendMessage(Component.text("El jugador ").color(NamedTextColor.RED)
                    .append(target.displayName())
                    .append(Component.text(" no ha iniciado sesión.").color(NamedTextColor.RED)));
            return;
        }

        // Lógica de gestión de solicitudes en TradeManager
        TradeManager.RequestResult result = tradeManager.requestTrade(requester, target);

        switch (result) {
            case OK -> {
                requester.sendMessage(Component.text("Has enviado una solicitud de trade a ").color(NamedTextColor.GREEN).append(target.displayName()));
                sendTradeNotification(requester, target);
            }
            case ALREADY_SENT ->
                    requester.sendMessage(Component.text("Ya has enviado una solicitud de trade a este jugador.").color(NamedTextColor.YELLOW));
            case TARGET_HAS_PENDING_REQUEST ->
                    requester.sendMessage(Component.text("Ese jugador ya tiene una solicitud pendiente, pídele que la acepte o cancele.").color(NamedTextColor.YELLOW));
            case IN_TRADE ->
                    requester.sendMessage(Component.text("Uno de los jugadores ya está en un trade activo.").color(NamedTextColor.RED));
        }
    }

    /**
     * Maneja la aceptación del trade: /trade accept <jugador>
     */
    private void handleTradeAccept(Player receiver, String requesterName) {
        TradeManager.AcceptResult result = tradeManager.acceptTrade(receiver, requesterName);

        switch (result) {
            case OK -> {
                // El TradeManager debe manejar la apertura de la GUI
                receiver.sendMessage(Component.text("Trade aceptado. Abriendo interfaz...").color(NamedTextColor.GREEN));
                // Mandar mensaje al requester (que ya debería estar almacenado en TradeManager)
                Player requester = Bukkit.getPlayer(tradeManager.getLastRequesterId(receiver.getUniqueId()));
                if (requester != null) {
                    requester.sendMessage(Component.text(receiver.getName()).color(NamedTextColor.GOLD)
                            .append(Component.text(" ha aceptado tu solicitud. Abriendo interfaz...").color(NamedTextColor.GREEN)));
                }
                // TODO: Aquí debes llamar a tu método de GUI. Por ejemplo:
                // plugin.getTradeGUI().openTradeGUI(requester, receiver);
            }
            case NO_PENDING_REQUEST ->
                    receiver.sendMessage(Component.text("No tienes una solicitud de trade pendiente de ese jugador.").color(NamedTextColor.RED));
            case REQUESTER_OFFLINE ->
                    receiver.sendMessage(Component.text("El jugador que envió la solicitud se desconectó.").color(NamedTextColor.RED));
            case ALREADY_IN_TRADE ->
                    receiver.sendMessage(Component.text("Ya estás en un trade activo.").color(NamedTextColor.RED));
        }
    }

    /**
     * Maneja la cancelación del trade: /trade cancel
     */
    private void handleTradeCancel(Player player) {
        if (tradeManager.cancelTrade(player)) {
            player.sendMessage(Component.text("Trade cancelado con éxito.").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No tienes solicitudes pendientes o trades activos para cancelar.").color(NamedTextColor.YELLOW));
        }
    }

    /**
     * NOTIFICACIÓN INTERACTIVA: Muestra el mensaje en el chat del receptor.
     */
    private void sendTradeNotification(Player requester, Player target) {
        // 1. Mensaje principal
        TextComponent baseMessage = Component.text(requester.getName()).color(NamedTextColor.GOLD)
                .append(Component.text(" quiere tradear contigo.")).color(NamedTextColor.YELLOW);

        // 2. Mensaje de aceptación clickeable
        TextComponent acceptClick = Component.text(" [ACEPTAR] ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/trade accept " + requester.getName()))
                .hoverEvent(Component.text("Click para aceptar el trade."));

        // 3. Instrucción alternativa
        TextComponent alternative = Component.text(" (o usa /trade accept ").color(NamedTextColor.GRAY)
                .append(Component.text(requester.getName()).color(NamedTextColor.AQUA))
                .append(Component.text(")").color(NamedTextColor.GRAY));

        // 4. Construir y enviar el mensaje final
        target.sendMessage(Component.empty());
        target.sendMessage(baseMessage.append(acceptClick).append(alternative));
        target.sendMessage(Component.empty());
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("--- Uso /Trade ---").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("/trade <jugador>").color(NamedTextColor.YELLOW).append(Component.text(" - Enviar solicitud de trade.")));
        player.sendMessage(Component.text("/trade accept <jugador>").color(NamedTextColor.YELLOW).append(Component.text(" - Aceptar la solicitud de un jugador.")));
        player.sendMessage(Component.text("/trade cancel").color(NamedTextColor.YELLOW).append(Component.text(" - Cancelar tu solicitud o trade activo.")));
    }
}