package com.vendraly.core.tradingui;

import com.vendraly.core.trade.TradeSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Construye inventarios de comercio seguro.
 */
public class TradeGuiManager {

    public TradeGuiManager() {
    }

    public TradeSession createSession(Player requester, Player target) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("Comercio Seguro"));
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text("", NamedTextColor.GRAY));
        filler.setItemMeta(fillerMeta);
        for (int slot = 21; slot <= 26; slot++) {
            inventory.setItem(slot, filler);
        }
        for (int slot = 45; slot <= 53; slot++) {
            inventory.setItem(slot, filler.clone());
        }

        ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = confirm.getItemMeta();
        meta.displayName(Component.text("Confirmar", NamedTextColor.GREEN));
        confirm.setItemMeta(meta);
        inventory.setItem(45, confirm);
        ItemStack confirmTarget = confirm.clone();
        inventory.setItem(53, confirmTarget);
        return new TradeSession(requester, target, inventory);
    }
}
