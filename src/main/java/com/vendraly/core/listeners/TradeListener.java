package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.trade.TradeGUI;
import com.vendraly.core.trade.TradeManager;
import com.vendraly.core.trade.TradeSession;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Maneja los clics y el cierre de los inventarios de tradeo, incluyendo los botones de dinero.
 */
public class TradeListener implements Listener {

    private final TradeManager tradeManager;
    private static final String TRADE_TITLE_IDENTIFIER = ChatColor.DARK_BLUE + ChatColor.BOLD.toString() + "Tradeo Seguro";

    // Slots que están permanentemente bloqueados (separadores, ofertas, botones, cancelar)
    private static final List<Integer> PERMANENTLY_BLOCKED_SLOTS = List.of(
            TradeGUI.P1_OFFER_SLOT, TradeGUI.P2_OFFER_SLOT,
            40, // Botón de Cancelar
            TradeGUI.P1_STATUS_SLOT, TradeGUI.P2_STATUS_SLOT, // Botones de Aceptar/Status
            TradeGUI.P1_INCREASE_10, TradeGUI.P1_INCREASE_1, TradeGUI.P1_DECREASE_10, TradeGUI.P1_DECREASE_1,
            TradeGUI.P2_INCREASE_10, TradeGUI.P2_INCREASE_1, TradeGUI.P2_DECREASE_10, TradeGUI.P2_DECREASE_1
    );

    public TradeListener(Main plugin) {
        this.tradeManager = plugin.getTradeManager();
    }

    /**
     * Captura los clics dentro del inventario de tradeo.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!tradeManager.isInTrade(player) || !event.getView().getTitle().equals(TRADE_TITLE_IDENTIFIER)) return;

        TradeSession session = tradeManager.getSession(player);
        if (session == null) return;

        Player otherPlayer = session.getOtherPlayer(player);
        Inventory tradeInv = event.getView().getTopInventory();
        int slot = event.getSlot();
        boolean isPlayer1 = player.equals(session.getPlayer1());

        List<Integer> playerSlots = isPlayer1 ? TradeGUI.PLAYER_SLOTS_1 : TradeGUI.PLAYER_SLOTS_2;
        List<Integer> otherSlots = isPlayer1 ? TradeGUI.PLAYER_SLOTS_2 : TradeGUI.PLAYER_SLOTS_1;


        // 1. Bloqueo de slots de control, separadores y slots del otro jugador
        if (TradeGUI.SEPARATOR_SLOTS.contains(slot) || otherSlots.contains(slot) ||
                PERMANENTLY_BLOCKED_SLOTS.contains(slot)) {
            event.setCancelled(true);
            if (otherSlots.contains(slot)) {
                player.sendMessage(ChatColor.RED + "Solo puedes modificar tus propios slots de tradeo.");
            }
            return;
        }


        // 2. Lógica de Control (Solo si el clic es en el Top Inventory)
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(tradeInv)) {

            // Permitir clics en slots de ítems propios (para que el ítem pueda entrar/salir)
            if (playerSlots.contains(slot)) {
                // Si el jugador clica en su propio slot, permitimos el movimiento del ítem
                // y reseteamos el estado.
                session.resetReadyStatus(player);
                return; // No cancelamos, permitimos que el ítem se mueva.
            }

            // A partir de aquí, el evento es en el Top Inventory (tradeInv) y no en slots de ítems propios,
            // por lo que debe ser un botón de control.
            event.setCancelled(true);

            // --- Lógica de Botones de Cantidad ---
            double amountToChange = 0.0;
            boolean isMoneyClick = false;

            // Se puede optimizar creando un mapa de slots a valores, pero la lógica actual es legible
            if (isPlayer1) {
                if (slot == TradeGUI.P1_INCREASE_10) { amountToChange = 10.0; isMoneyClick = true; }
                else if (slot == TradeGUI.P1_INCREASE_1) { amountToChange = 1.0; isMoneyClick = true; }
                else if (slot == TradeGUI.P1_DECREASE_10) { amountToChange = -10.0; isMoneyClick = true; }
                else if (slot == TradeGUI.P1_DECREASE_1) { amountToChange = -1.0; isMoneyClick = true; }
            } else { // Player 2
                if (slot == TradeGUI.P2_INCREASE_10) { amountToChange = 10.0; isMoneyClick = true; }
                else if (slot == TradeGUI.P2_INCREASE_1) { amountToChange = 1.0; isMoneyClick = true; }
                else if (slot == TradeGUI.P2_DECREASE_10) { amountToChange = -10.0; isMoneyClick = true; }
                else if (slot == TradeGUI.P2_DECREASE_1) { amountToChange = -1.0; isMoneyClick = true; }
            }

            if (isMoneyClick) {
                double currentOffer = session.getMoneyOffer(player);
                double newOffer = currentOffer + amountToChange;

                if (newOffer < 0) newOffer = 0.0;

                // setMoneyOffer verifica el límite de cash robable y resetea el estado
                if (session.setMoneyOffer(player, newOffer)) {
                    player.sendMessage(ChatColor.YELLOW + "Oferta de efectivo ajustada a: " + ChatColor.GOLD + String.format("$%.2f", session.getMoneyOffer(player)));
                }
                return;
            }
            // --- Fin de Lógica de Botones de Cantidad ---

            // Clic en Slots de ACEPTAR/STATUS
            if ((isPlayer1 && slot == TradeGUI.P1_STATUS_SLOT) || (!isPlayer1 && slot == TradeGUI.P2_STATUS_SLOT)) {
                session.toggleReady(player);
                return;
            }

            // Clic en CANCELAR (Slot 40)
            if (slot == 40) {
                player.sendMessage(ChatColor.RED + "Tradeo cancelado.");
                if (otherPlayer != null) {
                    otherPlayer.sendMessage(ChatColor.RED + player.getName() + " ha cancelado el tradeo.");
                }
                // returnItems y endTrade se llama al final.
                session.returnItems(tradeInv);
                tradeManager.endTrade(session);
                return;
            }
        }

        // 3. Resetear estado si hay movimiento de ítems desde/hacia el inventario del jugador
        // Si el evento no ha sido cancelado y el inventario cliqueado es el del jugador,
        // o si es un shift-click, es muy probable que un ítem se haya movido.
        if (!event.isCancelled() && (event.isShiftClick() ||
                (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())))) {

            // Reseteamos el estado en cualquier movimiento que involucre el inventario del jugador
            // Esto es crucial para la seguridad del tradeo.
            session.resetReadyStatus(player);
        }
    }

    /**
     * Maneja el arrastre de ítems para resetear el estado de listo y bloquear slots de control.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (tradeManager.isInTrade(player) && event.getView().getTitle().equals(TRADE_TITLE_IDENTIFIER)) {
            TradeSession session = tradeManager.getSession(player);
            if (session == null) return;

            Inventory tradeInv = event.getView().getTopInventory();
            List<Integer> playerSlots = player.equals(session.getPlayer1()) ? TradeGUI.PLAYER_SLOTS_1 : TradeGUI.PLAYER_SLOTS_2;

            for (int slot : event.getRawSlots()) {
                // Si el slot es parte del inventario de tradeo (Top Inventory)
                if (slot < tradeInv.getSize()) {
                    // Bloquear slots que NO son del jugador o son de control
                    if (!playerSlots.contains(slot) || TradeGUI.CONTROL_SLOTS.contains(slot) || TradeGUI.SEPARATOR_SLOTS.contains(slot)) {
                        event.setCancelled(true);
                        return;
                    }
                    // Si el arrastre es en los slots propios, reseteamos el estado
                    if (playerSlots.contains(slot)) {
                        session.resetReadyStatus(player);
                    }
                }
            }
        }
    }


    /**
     * Captura el cierre del inventario para finalizar el tradeo.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (tradeManager.isInTrade(player) && event.getView().getTitle().equals(TRADE_TITLE_IDENTIFIER)) {
            TradeSession session = tradeManager.getSession(player);

            if (session != null && session.getPlayers().contains(player)) {
                // Retornar los ítems
                session.returnItems(event.getInventory());

                // Mensajes y limpieza de la sesión
                Player other = session.getOtherPlayer(player);
                player.sendMessage(ChatColor.RED + "Tradeo cancelado por cierre de inventario.");
                if (other != null && other.isOnline()) { // Verificar si el otro sigue en línea
                    other.sendMessage(ChatColor.RED + player.getName() + " ha cerrado el inventario y cancelado el tradeo.");
                }

                // endTrade se encarga de cerrar el inventario del otro si es necesario.
                tradeManager.endTrade(session);
            }
        }
    }
}