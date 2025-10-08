package com.vendraly.listeners;

import com.vendraly.core.protection.ProtectionManager;
import com.vendraly.core.rpg.loot.LootTableManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener para cofres RPG.
 */
public class LootChestListener implements Listener {

    private final LootTableManager lootTableManager;
    private final ProtectionManager protectionManager;

    public LootChestListener(LootTableManager lootTableManager, ProtectionManager protectionManager) {
        this.lootTableManager = lootTableManager;
        this.protectionManager = protectionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() == Material.CHEST && event.getPlayer().isSneaking()) {
            lootTableManager.dropLoot(block.getLocation().add(0.5, 1, 0.5), org.bukkit.entity.EntityType.ZOMBIE);
        }
    }
}
