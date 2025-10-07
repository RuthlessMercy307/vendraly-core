package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener que valida si un jugador cumple con los requisitos RPG
 * (Fuerza, Velocidad, etc.) para usar armas y herramientas.
 */
public class ItemRequirementListener implements Listener {

    private final StatManager statManager;
    private final ItemMetadataKeys keys;

    public ItemRequirementListener(Main plugin) {
        this.statManager = plugin.getStatManager();
        this.keys = plugin.getItemMetadataKeys();
    }

    /**
     * Extrae un requisito del PDC de un ítem.
     * @param item ItemStack a verificar
     * @param key Clave PDC
     * @return valor del requisito, o 0 si no está
     */
    private int getPDCRequirement(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    // =========================================================
    //   BLOQUEO DE ATAQUE (Espadas / Hachas)
    // =========================================================
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) return;

        String type = heldItem.getType().name();
        if (!type.contains("SWORD") && !type.contains("AXE")) return;

        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        int minStrengthReq = getPDCRequirement(heldItem, keys.REQ_STAT_STRENGTH);
        int minSpeedReq = getPDCRequirement(heldItem, keys.REQ_STAT_SPEED);

        // Fuerza mínima (HARD STOP)
        if (minStrengthReq > 0 && stats.getStatStrength() < minStrengthReq) {
            event.setCancelled(true);
            player.sendActionBar(
                    Component.text("Tu Fuerza (" + stats.getStatStrength() + ") es muy baja. Necesitas " + minStrengthReq + " para usar esta arma.")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        // Velocidad mínima (SOFT WARNING)
        if (minSpeedReq > 0 && stats.getStatMovementSpeed() < minSpeedReq) {
            player.sendActionBar(
                    Component.text("Esta arma se siente pesada. Necesitas " + minSpeedReq + " de Velocidad para usarla a su máximo potencial.")
                            .color(NamedTextColor.YELLOW)
            );
        }
    }

    // =========================================================
    //   BLOQUEO DE RECOLECCIÓN (Picos / Hachas / Palas)
    // =========================================================
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) return;

        String type = heldItem.getType().name();
        if (!type.contains("PICKAXE") && !type.contains("AXE") && !type.contains("SHOVEL")) return;

        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        int minStrengthReq = getPDCRequirement(heldItem, keys.REQ_STAT_STRENGTH);

        // Fuerza mínima (HARD STOP)
        if (minStrengthReq > 0 && stats.getStatStrength() < minStrengthReq) {
            event.setCancelled(true);
            player.sendActionBar(
                    Component.text("Tu Fuerza (" + stats.getStatStrength() + ") es muy baja. Necesitas " + minStrengthReq + " para usar esta herramienta.")
                            .color(NamedTextColor.RED)
            );
        }
    }
}
