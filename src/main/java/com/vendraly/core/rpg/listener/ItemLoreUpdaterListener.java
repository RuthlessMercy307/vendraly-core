package com.vendraly.core.rpg.listener;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.rpg.item.ItemLoreUpdater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Escucha eventos de inventario para actualizar el Lore de los ítems
 * con los requisitos de estadísticas/límite de daño, aplicando colores
 * basados en las estadísticas actuales del jugador.
 */
public class ItemLoreUpdaterListener implements Listener {
    private final ItemLoreUpdater itemLoreUpdater;
    private final Main plugin;

    // Constante para identificar el menú de stats
    // Debes asegurarte que este título coincida con el título que usa MenuBuilder.java
    private static final String STATS_MENU_TITLE_PARTIAL = "Menú de Estadísticas";

    public ItemLoreUpdaterListener(Main plugin, ItemLoreUpdater itemLoreUpdater) {
        this.plugin = plugin;
        this.itemLoreUpdater = itemLoreUpdater;
    }

    // Se activa cuando un jugador abre cualquier inventario
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Evitar procesar el menú de stats
        if (event.getView().getTitle().equalsIgnoreCase(STATS_MENU_TITLE_PARTIAL)) {
            return;
        }

        PlayerData data = plugin.getAuthManager().getPlayerData(player.getUniqueId());
        if (data == null || data.getRpgStats() == null) return;

        // Inventario abierto
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                itemLoreUpdater.updateLoreAndPDC(item, player, data.getRpgStats());
            }
        }

        // Inventario del jugador (hotbar, armadura, etc.)
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                itemLoreUpdater.updateLoreAndPDC(item, player, data.getRpgStats());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Evitar procesar el menú de stats
        if (event.getView().getTitle().equalsIgnoreCase(STATS_MENU_TITLE_PARTIAL)) {
            return;
        }

        PlayerData data = plugin.getAuthManager().getPlayerData(player.getUniqueId());
        if (data == null || data.getRpgStats() == null) return;

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            itemLoreUpdater.updateLoreAndPDC(cursor, player, data.getRpgStats());
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir()) {
            itemLoreUpdater.updateLoreAndPDC(clicked, player, data.getRpgStats());
        }
    }
}