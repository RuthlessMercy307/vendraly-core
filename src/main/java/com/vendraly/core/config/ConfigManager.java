package com.vendraly.core.config;

import com.vendraly.VendralyCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona archivos de configuración adicionales del plugin, como jobs, loot y roles.
 */
public class ConfigManager {

    private final VendralyCore plugin;
    private final Map<String, FileConfiguration> configurations = new ConcurrentHashMap<>();

    public ConfigManager(VendralyCore plugin) {
        this.plugin = plugin;
        load("jobs.yml");
        load("loot.yml");
        load("roles.yml");
        load("clans.yml");
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration get(String fileName) {
        return configurations.computeIfAbsent(fileName, this::load);
    }

    private FileConfiguration load(String fileName) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File file = new File(dataFolder, fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configurations.put(fileName, configuration);
        return configuration;
    }

    public void save(String fileName) {
        FileConfiguration configuration = configurations.get(fileName);
        if (configuration == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), fileName);
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("No se pudo guardar el archivo " + fileName + ": " + e.getMessage());
        }
    }

    public void saveAll(List<String> fileNames) {
        fileNames.forEach(this::save);
    }
}
