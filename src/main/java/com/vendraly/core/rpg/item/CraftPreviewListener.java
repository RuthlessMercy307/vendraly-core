package com.vendraly.core.rpg.item;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.ability.AbilityType;
import com.vendraly.core.rpg.item.CraftingManager.ItemCategory;
import com.vendraly.core.rpg.item.CraftingManager.ItemContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CraftPreviewListener implements Listener {

    private final Main plugin;
    private final ItemMetadataKeys keys;
    private final StatManager statManager;

    public CraftPreviewListener(Main plugin, ItemMetadataKeys keys, StatManager statManager) {
        this.plugin = plugin;
        this.keys = keys;
        this.statManager = statManager;
    }

    @EventHandler
    public void onCraftPreview(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null || result.getType() == Material.AIR) return;

        if (!(event.getView().getPlayer() instanceof Player player)) return;

        // Obtener stats del jugador
        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        // Determinar categoría (muy básico: armas, armaduras o tools)
        ItemCategory category = guessCategory(result.getType());

        // Contexto de crafteo: material + categoría + nivel skill
        ItemContext ctx = new ItemContext(
                result.getType(),
                category,
                stats.getAbilityLevel(AbilityType.BLACKSMITHING) // usa tu sistema de habilidades
        );

        // Construir ítem con CraftingManager (incluye atributos, calidad, reqs, durabilidad)
        ItemStack crafted = CraftingManager.buildCraftedItem(
                plugin, keys, result, AbilityType.BLACKSMITHING, ctx
        );

        // Aplicar lore/atributos visuales finales
        new ItemLoreUpdater(plugin, keys).updateLoreAndPDC(crafted, player, stats);

        inv.setResult(crafted);
    }

    /** Detecta la categoría del item a partir de su Material. */
    private ItemCategory guessCategory(Material mat) {
        String n = mat.name();
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_BOW")) {
            return ItemCategory.WEAPON;
        } else if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") ||
                n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) {
            return ItemCategory.ARMOR;
        } else {
            return ItemCategory.TOOL;
        }
    }
}
