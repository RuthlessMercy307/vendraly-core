package com.vendraly.listeners;

import com.vendraly.core.trade.TradeManager;
import com.vendraly.core.trade.TradeSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Listener que valida interacciones dentro de la GUI de trade.
 */
public class TradeListener implements Listener {

    private final TradeManager tradeManager;
    private final com.vendraly.core.tradingui.TradeGuiManager tradeGuiManager;

    public TradeListener(TradeManager tradeManager, com.vendraly.core.tradingui.TradeGuiManager tradeGuiManager) {
        this.tradeManager = tradeManager;
        this.tradeGuiManager = tradeGuiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        TradeSession session = tradeManager.getSession(player);
        if (session == null) {
            return;
        }
        if (!event.getInventory().equals(session.getInventory())) {
            return;
        }
        if (event.getSlot() == 45 || event.getSlot() == 53) {
            session.toggleReady(player);
            if (session.isReady()) {
                tradeManager.endSession(session, true);
                session.getRequester().closeInventory();
                session.getTarget().closeInventory();
            }
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() >= 54) {
            return; // inventario del jugador
        }
        boolean allowed = player.equals(session.getRequester())
                ? contains(session.getRequesterSlots(), event.getRawSlot())
                : contains(session.getTargetSlots(), event.getRawSlot());
        if (!allowed) {
            event.setCancelled(true);
        } else {
            session.resetReady();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        TradeSession session = tradeManager.getSession(player);
        if (session != null) {
            tradeManager.endSession(session, false);
        }
    }

    private boolean contains(int[] slots, int slot) {
        for (int value : slots) {
            if (value == slot) {
                return true;
            }
        }
        return false;
    }
}
