package com.vendraly.listeners;

import com.vendraly.core.rpg.combat.AttackDirection;
import com.vendraly.core.rpg.combat.CombatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener principal del combate direccional.
 */
public class CombatListener implements Listener {

    private final CombatManager combatManager;

    public CombatListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        combatManager.setDirection(event.getPlayer(), AttackDirection.fromYaw(event.getPlayer().getLocation().getYaw()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player attacker) {
            if (!combatManager.consumeStamina(attacker, 5.0)) {
                event.setCancelled(true);
                return;
            }
            double damage = combatManager.computeDamage(attacker);
            if (event.getEntity() instanceof org.bukkit.entity.Player defender) {
                double defense = combatManager.computeDefense(defender);
                damage = Math.max(0.5, damage - (defense / 10.0));
            }
            event.setDamage(damage);
        }
    }
}
