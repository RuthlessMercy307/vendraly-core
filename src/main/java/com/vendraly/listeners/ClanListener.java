package com.vendraly.listeners;

import com.vendraly.core.clans.Clan;
import com.vendraly.core.clans.ClanManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener para efectos de guerra y saqueos.
 */
public class ClanListener implements Listener {

    private final ClanManager clanManager;
    public ClanListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player victim && event.getDamager() instanceof org.bukkit.entity.Player attacker) {
            Clan attackerClan = clanManager.getClanByPlayer(attacker.getUniqueId());
            Clan victimClan = clanManager.getClanByPlayer(victim.getUniqueId());
            if (attackerClan == null || victimClan == null) {
                return;
            }
            if (attackerClan.equals(victimClan)) {
                event.setCancelled(true);
                attacker.sendMessage("No puedes dañar a miembros de tu clan.");
                return;
            }
            if (!clanManager.isAtWar(attackerClan.getId(), victimClan.getId())) {
                attacker.sendMessage("Debes declarar la guerra antes de atacar a ese clan.");
                event.setCancelled(true);
            }
        }
    }
}
