package com.vendraly.core.rpg.listener;

import com.vendraly.core.jobs.JobManager;
import com.vendraly.core.rpg.ability.AbilityManager;
import com.vendraly.core.rpg.ability.AbilityType;
import com.vendraly.core.rpg.loot.LootTableManager;
import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stats.StatType;
import com.vendraly.core.rpg.stats.XPManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener para eventos RPG que otorgan experiencia y loot.
 */
public class RPGPlayerListener implements Listener {

    private final StatManager statManager;
    private final XPManager xpManager;
    private final JobManager jobManager;
    private final LootTableManager lootTableManager;
    private final AbilityManager abilityManager;

    public RPGPlayerListener(StatManager statManager, XPManager xpManager, JobManager jobManager, LootTableManager lootTableManager, AbilityManager abilityManager) {
        this.statManager = statManager;
        this.xpManager = xpManager;
        this.jobManager = jobManager;
        this.lootTableManager = lootTableManager;
        this.abilityManager = abilityManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        jobManager.reward(event.getPlayer(), material, xpManager);
        statManager.rewardAction(event.getPlayer(), StatType.STRENGTH, 0.1, 2);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            xpManager.addExperience(event.getEntity().getKiller(), 10);
            lootTableManager.dropLoot(event.getEntity().getLocation(), event.getEntity().getType());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.BLAZE_ROD) {
            abilityManager.trigger(event.getPlayer(), AbilityType.FIREBALL);
        }
    }
}
