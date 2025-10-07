package com.vendraly.core.rpg.listener;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGItemGenerator;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.ability.AbilityType;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import com.vendraly.core.rpg.item.ItemLoreUpdater;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Escucha eventos de crafteo para aplicar la lógica RPG de generación de ítems.
 */
public class CraftingListener implements Listener {

    private final Main plugin;
    private final StatManager statManager;
    private final ItemLoreUpdater itemLoreUpdater;
    private final RPGItemGenerator itemGenerator;
    private final ItemMetadataKeys keys;

    public CraftingListener(Main plugin, StatManager statManager, ItemLoreUpdater itemLoreUpdater, ItemMetadataKeys keys) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.itemLoreUpdater = itemLoreUpdater;
        this.keys = keys;
        this.itemGenerator = new RPGItemGenerator(plugin, keys);
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null) return;

        // Solo procesamos materiales RPG relevantes
        if (!itemLoreUpdater.isRPGEligibleMaterial(result.getType())) return;

        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        Material material = result.getType();
        String name = material.name();

        // Determinar habilidad
        AbilityType ability;
        if (name.contains("_SWORD") || name.contains("_PICKAXE") || name.contains("_AXE") || name.contains("_SHOVEL") || name.contains("_HOE")) {
            ability = AbilityType.BLACKSMITHING;
        } else if (name.contains("_HELMET") || name.contains("_CHESTPLATE") || name.contains("_LEGGINGS") || name.contains("_BOOTS")) {
            ability = AbilityType.TAILORING;
        } else {
            return;
        }

        // Definir dificultad base según material
        int difficultyLevel, strReq, defReq, maxLimit;
        if (name.contains("NETHERITE")) { difficultyLevel = 50; strReq = 15; defReq = 15; maxLimit = 75; }
        else if (name.contains("DIAMOND")) { difficultyLevel = 40; strReq = 10; defReq = 10; maxLimit = 50; }
        else if (name.contains("IRON")) { difficultyLevel = 25; strReq = 5; defReq = 5; maxLimit = 25; }
        else if (name.contains("GOLD") || name.contains("STONE")) { difficultyLevel = 15; strReq = 3; defReq = 3; maxLimit = 15; }
        else if (name.contains("LEATHER") || name.contains("WOOD")) { difficultyLevel = 5; strReq = 1; defReq = 1; maxLimit = 10; }
        else { difficultyLevel = 1; strReq = 0; defReq = 0; maxLimit = 10; }

        // Base data para PDC
        Map<String, Integer> basePDCData = new HashMap<>();
        basePDCData.put(keys.REQ_STAT_STRENGTH.getKey(), strReq);
        basePDCData.put(keys.REQ_STAT_DEFENSE.getKey(), defReq);
        basePDCData.put(keys.STAT_LIMIT_MAX_DMG.getKey(), maxLimit);

        if (ability == AbilityType.BLACKSMITHING) {
            basePDCData.put(keys.REQ_SKILL_BLACKSMITHING.getKey(), difficultyLevel);
            basePDCData.put(keys.REQ_SKILL_TAILORING.getKey(), 0);
        } else {
            basePDCData.put(keys.REQ_SKILL_TAILORING.getKey(), difficultyLevel);
            basePDCData.put(keys.REQ_SKILL_BLACKSMITHING.getKey(), 0);
        }

        // Nivel de habilidad del jugador
        int playerSkillLevel = stats.getAbilityLevel(ability);

        // Generar ítem RPG
        ItemStack finalItem = itemGenerator.generateRPGItem(
                result.clone(),
                playerSkillLevel,
                ability,
                difficultyLevel,
                basePDCData
        );

        // Actualizar lore inmediatamente
        itemLoreUpdater.updateLoreAndPDC(finalItem, player, stats);

        // --- Sustituir resultado del crafteo ---
        inv.setResult(finalItem);

        // Manejo de shift-click (crafteo en masa)
        if (event.isShiftClick()) {
            // Bloqueamos por ahora para evitar exploits
            event.setCancelled(true);
            player.sendMessage("§cEl crafteo en masa aún no está soportado en el sistema RPG.");
        }
    }
}
