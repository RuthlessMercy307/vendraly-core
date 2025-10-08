package com.vendraly.listeners;

import com.vendraly.core.jobs.JobManager;
import com.vendraly.core.rpg.stats.XPManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

/**
 * Listener para recompensar acciones de oficios.
 */
public class JobListener implements Listener {

    private final JobManager jobManager;
    private final XPManager xpManager;

    public JobListener(JobManager jobManager, XPManager xpManager) {
        this.jobManager = jobManager;
        this.xpManager = xpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        jobManager.reward(event.getPlayer(), event.getBlock().getType(), xpManager);
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof org.bukkit.entity.Player player && event.getRecipe() != null) {
            jobManager.reward(player, event.getRecipe().getResult().getType(), xpManager);
        }
    }
}
