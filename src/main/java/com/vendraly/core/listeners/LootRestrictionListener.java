package com.vendraly.core.listeners;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent; // Nuevo
import org.bukkit.event.inventory.InventoryType; // Nuevo
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.Inventory; // Nuevo
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Arrays;
import java.util.List;

public class LootRestrictionListener implements Listener {

    /**
     * Verifica si un ItemStack es un libro encantado que contiene Mending.
     */
    private boolean isMendingBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        // Verifica si el libro encantado almacena el encantamiento MENDING
        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            return meta.hasStoredEnchant(Enchantment.MENDING);
        }
        return false;
    }

    // --- BLOQUEO DE PESCA (Se mantiene el código anterior) ---
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (event.getCaught() instanceof Item caughtItem) {
                ItemStack item = caughtItem.getItemStack();

                if (isMendingBook(item)) {
                    event.setCancelled(true);
                    caughtItem.remove();

                    event.getPlayer().sendMessage("§c[Vendraly] El libro de Reparación (Mending) está prohibido y ha sido destruido.");
                }
            }
        }
    }

    // --- BLOQUEO DE BOTÍN DE COFRES (Alternativa compatible con Spigot/Bukkit) ---
    // Usamos este evento porque solo se dispara una vez (al abrirse el inventario por primera vez)
    // para inventarios generados con botín aleatorio.
    @EventHandler
    public void onLootChestOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();

        // Identificar los tipos de inventario que pueden contener botín de Mending
        // (Por ejemplo: Cofres, barriles, shulker boxes, etc., si fueron generados con botín)
        // La lógica de Bukkit/Spigot para detectar cofres de botín es limitada.
        // Nos enfocaremos en los que tienen loot table:
        List<InventoryType> lootTypes = Arrays.asList(
                InventoryType.CHEST, // Cofre normal
                InventoryType.BARREL  // Barril
                // Otros tipos como SHULKER_BOX también podrían ser botín.
        );

        if (!lootTypes.contains(inventory.getType())) {
            return;
        }

        boolean mendingFound = false;

        // Iterar sobre todos los slots del inventario
        for (ItemStack item : inventory.getContents()) {
            if (isMendingBook(item)) {
                // Si encontramos Mending, lo eliminamos y notificamos
                inventory.remove(item);
                mendingFound = true;
            }
        }

        if (mendingFound) {
            player.sendMessage("§c[Vendraly] ¡Advertencia! El libro de Reparación (Mending) fue detectado y eliminado de este cofre.");
        }
    }
}