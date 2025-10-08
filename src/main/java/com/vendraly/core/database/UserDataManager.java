package com.vendraly.core.database;

import com.vendraly.VendralyCore;
import com.vendraly.core.jobs.JobProgress;
import com.vendraly.core.roles.Role;
import com.vendraly.core.rpg.stats.RPGStats;
import com.vendraly.core.rpg.stats.StatType;
import com.vendraly.utils.TaskUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga y guarda información persistente de jugadores en archivos YAML.
 */
public class UserDataManager {

    private final VendralyCore plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public UserDataManager(VendralyCore plugin) {
        this.plugin = plugin;
    }

    public PlayerData getOrCreate(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, id -> load(uuid, name));
    }

    public void save(PlayerData data) {
        TaskUtil.runAsync(plugin, () -> saveSync(data));
    }

    public void saveAll() {
        cache.values().forEach(this::saveSync);
    }

    private void saveSync(PlayerData data) {
        File file = getFile(data.getUuid());
        FileConfiguration config = new YamlConfiguration();
        config.set("name", data.getName());
        config.set("password", data.getPasswordHash());
        config.set("role", data.getRole().name());
        config.set("bank", data.getBankBalance());
        config.set("cash", data.getCashBalance());
        config.set("clan", data.getClanId());
        config.set("rpg.level", data.getRpgLevel());
        config.set("rpg.experience", data.getRpgExperience());
        config.set("rpg.unspent", data.getUnspentPoints());

        RPGStats stats = data.getStats();
        for (StatType type : StatType.values()) {
            config.set("stats." + type.name().toLowerCase(), stats.getStat(type));
        }

        Map<String, Object> jobSection = new HashMap<>();
        for (JobProgress progress : data.getJobs().values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("experience", progress.getExperience());
            map.put("level", progress.getLevel());
            jobSection.put(progress.getJobId(), map);
        }
        config.createSection("jobs", jobSection);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("No se pudo guardar datos de " + data.getName() + ": " + e.getMessage());
        }
    }

    private PlayerData load(UUID uuid, String name) {
        File file = getFile(uuid);
        if (!file.exists()) {
            return new PlayerData(uuid, name);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid, config.getString("name", name));
        data.setPasswordHash(config.getString("password", ""));
        data.setRole(Role.valueOf(config.getString("role", Role.CIVILIAN.name())));
        data.setBankBalance(config.getDouble("bank", 0.0));
        data.setCashBalance(config.getDouble("cash", 0.0));
        data.setClanId(config.getString("clan"));
        data.setRpgLevel(config.getInt("rpg.level", 1));
        data.setRpgExperience(config.getLong("rpg.experience", 0L));
        data.setUnspentPoints(config.getInt("rpg.unspent", 0));

        RPGStats stats = data.getStats();
        for (StatType type : StatType.values()) {
            stats.setStat(type, config.getDouble("stats." + type.name().toLowerCase(), type.getBaseValue()));
        }

        if (config.isConfigurationSection("jobs")) {
            for (String key : config.getConfigurationSection("jobs").getKeys(false)) {
                JobProgress progress = data.getOrCreateJob(key);
                progress.addExperience(config.getLong("jobs." + key + ".experience", 0L));
                progress.setLevel(config.getInt("jobs." + key + ".level", 1));
            }
        }

        data.setBanned(config.getBoolean("banned", false));
        return data;
    }

    private File getFile(UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "userdata");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, uuid + ".yml");
    }
}
