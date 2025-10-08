package com.vendraly.core.protection;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Permite proteger cofres usando un ítem específico.
 */
public class ProtectionListener implements Listener {

    private final ProtectionManager protectionManager;

    public ProtectionListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getPlayer().isSneaking() && event.getItem() != null && event.getItem().getType() == Material.STICK) {
                if (event.getClickedBlock().getType() == Material.CHEST || event.getClickedBlock().getType() == Material.BARREL) {
                    protectionManager.protectChest(event.getPlayer(), event.getClickedBlock().getLocation());
                    event.getPlayer().sendMessage("Protegiste este contenedor para tu clan.");
                }
            }
        }
    }
}
