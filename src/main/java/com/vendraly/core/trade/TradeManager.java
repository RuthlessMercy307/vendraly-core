package com.vendraly.core.trade;

import com.vendraly.VendralyCore;
import com.vendraly.core.economy.CashManager;
import com.vendraly.core.tradingui.TradeGuiManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coordina solicitudes de comercio y sesiones activas.
 */
public class TradeManager {

    private final VendralyCore plugin;
    private final CashManager cashManager;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public TradeManager(VendralyCore plugin, CashManager cashManager) {
        this.plugin = plugin;
        this.cashManager = cashManager;
    }

    public void requestTrade(Player requester, Player target) {
        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());
        target.sendMessage(requester.getName() + " quiere comerciar contigo. Usa /trade accept " + requester.getName());
        requester.sendMessage("Solicitud enviada a " + target.getName());
    }

    public boolean accept(Player target, Player requester) {
        UUID stored = pendingRequests.get(target.getUniqueId());
        if (stored == null || !stored.equals(requester.getUniqueId())) {
            return false;
        }
        pendingRequests.remove(target.getUniqueId());
        TradeSession session = plugin.getTradeGuiManager().createSession(requester, target);
        sessions.put(requester.getUniqueId(), session);
        sessions.put(target.getUniqueId(), session);
        requester.openInventory(session.getInventory());
        target.openInventory(session.getInventory());
        return true;
    }

    public TradeSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean offerMoney(Player player, double amount) {
        TradeSession session = getSession(player);
        if (session == null) {
            return false;
        }
        if (amount < 0) {
            return false;
        }
        double balance = cashManager.getBalance(player.getUniqueId());
        if (balance < amount) {
            return false;
        }
        session.setMoney(player, amount);
        player.sendMessage("Oferta monetaria establecida en " + amount);
        Player partner = player.equals(session.getRequester()) ? session.getTarget() : session.getRequester();
        partner.sendMessage(player.getName() + " ofrece " + amount + " monedas en el intercambio");
        return true;
    }

    public void endSession(TradeSession session, boolean success) {
        sessions.remove(session.getRequester().getUniqueId());
        sessions.remove(session.getTarget().getUniqueId());
        if (success) {
            if (!hasFunds(session)) {
                returnItems(session);
                session.getRequester().sendMessage("Intercambio cancelado: fondos insuficientes");
                session.getTarget().sendMessage("Intercambio cancelado: fondos insuficientes");
            } else {
                transferItems(session);
                transferMoney(session);
                session.getRequester().sendMessage("Intercambio completado");
                session.getTarget().sendMessage("Intercambio completado");
            }
        } else {
            returnItems(session);
            session.getRequester().sendMessage("Intercambio cancelado");
            session.getTarget().sendMessage("Intercambio cancelado");
        }
    }

    public void tick() {
        // Se podría manejar expiraciones.
    }

    public void shutdown() {
        sessions.values().forEach(session -> {
            returnItems(session);
            session.getRequester().closeInventory();
            session.getTarget().closeInventory();
        });
        sessions.clear();
        pendingRequests.clear();
    }

    private boolean hasFunds(TradeSession session) {
        double requesterOffer = session.getMoney(session.getRequester());
        double targetOffer = session.getMoney(session.getTarget());
        return cashManager.getBalance(session.getRequester().getUniqueId()) >= requesterOffer
                && cashManager.getBalance(session.getTarget().getUniqueId()) >= targetOffer;
    }

    private void transferItems(TradeSession session) {
        Inventory inventory = session.getInventory();
        Player requester = session.getRequester();
        Player target = session.getTarget();
        for (int slot : session.getRequesterSlots()) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                target.getInventory().addItem(stack.clone());
            }
            inventory.clear(slot);
        }
        for (int slot : session.getTargetSlots()) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                requester.getInventory().addItem(stack.clone());
            }
            inventory.clear(slot);
        }
    }

    private void transferMoney(TradeSession session) {
        double requesterOffer = session.getMoney(session.getRequester());
        double targetOffer = session.getMoney(session.getTarget());
        if (requesterOffer > 0) {
            cashManager.modify(session.getRequester().getUniqueId(), -requesterOffer);
            cashManager.modify(session.getTarget().getUniqueId(), requesterOffer);
        }
        if (targetOffer > 0) {
            cashManager.modify(session.getTarget().getUniqueId(), -targetOffer);
            cashManager.modify(session.getRequester().getUniqueId(), targetOffer);
        }
    }

    private void returnItems(TradeSession session) {
        Inventory inventory = session.getInventory();
        for (int slot : session.getRequesterSlots()) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                session.getRequester().getInventory().addItem(stack.clone());
            }
            inventory.clear(slot);
        }
        for (int slot : session.getTargetSlots()) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                session.getTarget().getInventory().addItem(stack.clone());
            }
            inventory.clear(slot);
        }
    }
}
