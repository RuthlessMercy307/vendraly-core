package com.vendraly.core.rpg.loot;

import com.vendraly.VendralyCore;
import com.vendraly.core.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Gestiona tablas de botín configurables.
 */
public class LootTableManager {

    private final VendralyCore plugin;
    private final Map<String, List<LootTableEntry>> tables = new HashMap<>();

    public LootTableManager(VendralyCore plugin) {
        this.plugin = plugin;
        load(plugin.getConfigManager());
    }

    private void load(ConfigManager configManager) {
        FileConfiguration config = configManager.get("loot.yml");
        if (!config.isConfigurationSection("tables")) {
            return;
        }
        for (String id : config.getConfigurationSection("tables").getKeys(false)) {
            List<LootTableEntry> entries = new ArrayList<>();
            ConfigurationSection section = config.getConfigurationSection("tables." + id);
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(section.getString(key + ".material", "STONE"));
                int min = section.getInt(key + ".min", 1);
                int max = section.getInt(key + ".max", 1);
                LootRarity rarity = LootRarity.valueOf(section.getString(key + ".rarity", "COMMON").toUpperCase());
                entries.add(new LootTableEntry(material, min, max, rarity));
            }
            tables.put(id.toLowerCase(), entries);
        }
    }

    public void dropLoot(Location location, EntityType type) {
        List<LootTableEntry> entries = tables.get(type.name().toLowerCase());
        if (entries == null) {
            return;
        }
        LootTableEntry entry = pick(entries);
        if (entry != null) {
            location.getWorld().dropItemNaturally(location, entry.createStack());
        }
    }

    private LootTableEntry pick(List<LootTableEntry> entries) {
        double totalWeight = entries.stream().mapToDouble(entry -> entry.getRarity().getWeight()).sum();
        double roll = Math.random() * totalWeight;
        for (LootTableEntry entry : entries) {
            roll -= entry.getRarity().getWeight();
            if (roll <= 0) {
                return entry;
            }
        }
        return entries.isEmpty() ? null : entries.get(0);
    }

    public Map<String, List<LootTableEntry>> getTables() {
        return tables;
    }

    public record LootTableEntry(Material material, int min, int max, LootRarity rarity) {
        public ItemStack createStack() {
            int amount = min + new Random().nextInt(Math.max(1, (max - min) + 1));
            return new ItemStack(material, amount);
        }
    }
}
