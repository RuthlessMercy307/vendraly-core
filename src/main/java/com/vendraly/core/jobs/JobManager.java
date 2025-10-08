package com.vendraly.core.jobs;

import com.vendraly.VendralyCore;
import com.vendraly.core.config.ConfigManager;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.rpg.stats.XPManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona oficios configurables.
 */
public class JobManager {

    private final Map<String, JobDefinition> jobs = new ConcurrentHashMap<>();
    private final UserDataManager userDataManager;

    public JobManager(VendralyCore plugin) {
        this.userDataManager = plugin.getUserDataManager();
        loadJobs(plugin.getConfigManager());
    }

    private void loadJobs(ConfigManager configManager) {
        FileConfiguration config = configManager.get("jobs.yml");
        if (!config.isConfigurationSection("jobs")) {
            return;
        }
        for (String id : config.getConfigurationSection("jobs").getKeys(false)) {
            String display = config.getString("jobs." + id + ".name", id);
            long reward = config.getLong("jobs." + id + ".reward", 5L);
            JobDefinition definition = new JobDefinition(id, display, reward);
            for (String mat : config.getStringList("jobs." + id + ".materials")) {
                Material material = Material.matchMaterial(mat);
                if (material != null) {
                    definition.getMaterials().add(material);
                }
            }
            jobs.put(id.toLowerCase(), definition);
        }
    }

    public Optional<JobDefinition> findJobByMaterial(Material material) {
        return jobs.values().stream().filter(job -> job.getMaterials().contains(material)).findFirst();
    }

    public Collection<JobDefinition> getJobs() {
        return jobs.values();
    }

    public void reward(Player player, Material material, XPManager xpManager) {
        findJobByMaterial(material).ifPresent(job -> {
            PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
            JobProgress progress = data.getOrCreateJob(job.getId());
            progress.addExperience(job.getBaseReward());
            xpManager.addExperience(player, job.getBaseReward());
            data.setCashBalance(data.getCashBalance() + job.getBaseReward());
            userDataManager.save(data);
            player.sendMessage("Ganaste " + job.getBaseReward() + " monedas por tu oficio de " + job.getDisplayName());
        });
    }
}
