package com.vendraly.listeners;

import com.vendraly.core.economy.CashManager;
import com.vendraly.core.economy.EconomyManager;
import com.vendraly.core.protection.ProtectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listeners generales relacionados con economía y protección de cofres.
 */
@SuppressWarnings("deprecation")
public class EconomyListener implements Listener {

    private final CashManager cashManager;
    private final EconomyManager economyManager;
    private final ProtectionManager protectionManager;

    public EconomyListener(CashManager cashManager, EconomyManager economyManager, ProtectionManager protectionManager) {
        this.cashManager = cashManager;
        this.economyManager = economyManager;
        this.protectionManager = protectionManager;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        double value = protectionManager.getCurrencyValue(event.getItem().getItemStack());
        if (value > 0) {
            cashManager.modify(player.getUniqueId(), value);
            event.getItem().remove();
            player.sendMessage("Convertiste botín en " + value + " monedas.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        economyManager.getBalance(player);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (!protectionManager.canAccessChest(player, event.getInventory().getLocation())) {
                event.setCancelled(true);
                player.sendMessage("Este cofre está protegido.");
            }
        }
    }
}
