package com.vendraly.core.database;

import com.vendraly.core.Main;
import com.vendraly.core.roles.Role;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.ability.AbilityType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Maneja la LECTURA y ESCRITURA de datos de jugadores en archivos YAML.
 * Actúa como la capa I/O, NO gestiona el estado en memoria (ese es trabajo del AuthManager).
 */
public class UserDataManager {
    private final Main plugin;
    private final File userDataFolder;

    // CACHÉ LIGERO: Solo almacena las configuraciones YAML cargadas para acceso rápido al disco.
    private final Map<UUID, YamlConfiguration> cachedConfigs = new HashMap<>();

    public UserDataManager(Main plugin) {
        this.plugin = plugin;
        this.userDataFolder = new File(plugin.getDataFolder(), "userData");

        if (!userDataFolder.exists()) {
            userDataFolder.mkdirs();
        }
    }

    // --- MANEJO BÁSICO DE ARCHIVOS ---

    public YamlConfiguration getPlayerConfig(UUID uuid) {
        if (cachedConfigs.containsKey(uuid)) {
            return cachedConfigs.get(uuid);
        }

        File playerFile = new File(userDataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo de datos para " + uuid + ": " + e.getMessage());
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        cachedConfigs.put(uuid, config);
        return config;
    }

    public void savePlayerConfig(UUID uuid) {
        YamlConfiguration config = cachedConfigs.get(uuid);
        if (config == null) return;

        File playerFile = new File(userDataFolder, uuid.toString() + ".yml");
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de " + uuid + ": " + e.getMessage());
        }
    }

    // --- LÓGICA DE CARGA Y GUARDADO ---

    public PlayerData loadPlayerData(UUID uuid, String username) {
        YamlConfiguration config = getPlayerConfig(uuid);
        PlayerData data = new PlayerData(uuid, username);

        if (!config.contains("player.first-join")) {
            initializeNewPlayer(config, uuid, username);
        }

        // Auth
        data.setRegistered(config.getBoolean("auth.registered", false));
        data.setPasswordHash(config.getString("auth.password-hash", null));
        data.setFailedAttempts(config.getInt("auth.failed-attempts", 0));
        data.setLockedUntil(config.getLong("auth.locked-until", 0));
        data.setLoggedIn(config.getBoolean("auth.is-logged", false));

        // Economy
        data.setBalance(config.getDouble("economy.safe-balance",
                plugin.getConfig().getDouble("economy.starting-balance", 100.0)));
        data.setCashBalance(config.getDouble("economy.cash-balance", 0.0));

        // Rol
        String roleName = config.getString("roles.current-role", "PLAYER");
        try {
            data.setCurrentRole(Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            data.setCurrentRole(Role.PLAYER);
        }

        // RPG
        loadRPGStats(config, data.getRpgStats());

        // Último login y nombre
        config.set("player.last-login", System.currentTimeMillis());
        config.set("player.last-known-name", username);
        savePlayerConfig(uuid);

        return data;
    }

    /** Versión asíncrona para no bloquear el main thread */
    public CompletableFuture<PlayerData> loadPlayerDataAsync(UUID uuid, String username) {
        return CompletableFuture.supplyAsync(() -> loadPlayerData(uuid, username));
    }

    public void savePlayerData(PlayerData data) {
        UUID uuid = data.getPlayerUUID();
        YamlConfiguration config = getPlayerConfig(uuid);

        config.set("player.name", data.getPlayerName());
        config.set("player.last-known-name", data.getPlayerName());
        config.set("player.last-updated", System.currentTimeMillis());

        config.set("auth.registered", data.isRegistered());
        config.set("auth.password-hash", data.getPasswordHash());
        config.set("auth.failed-attempts", data.getFailedAttempts());
        config.set("auth.locked-until", data.getLockedUntil());
        config.set("auth.is-logged", data.isLoggedIn());

        config.set("economy.safe-balance", data.getBalance());
        config.set("economy.cash-balance", data.getCashBalance());

        config.set("roles.current-role", data.getCurrentRole().name());

        saveRPGStats(config, data.getRpgStats());

        savePlayerConfig(uuid);
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerConfig(uuid);
        cachedConfigs.remove(uuid);
    }

    // --- RPG ---

    private void loadRPGStats(YamlConfiguration config, RPGStats stats) {
        ConfigurationSection section = config.getConfigurationSection("rpg.stats");
        if (section == null) return;

        stats.setLevel(section.getInt("level", stats.getLevel()));
        stats.setTotalExperience(section.getLong("total-exp", stats.getTotalExperience()));
        stats.setUnspentPoints(section.getInt("unspent-points", stats.getUnspentPoints()));
        stats.setMaxVanillaLevelReached(section.getInt("max-vanilla-level", stats.getMaxVanillaLevelReached()));

        stats.setStatHealth(section.getInt("stat-health", stats.getStatHealth()));
        stats.setStatStrength(section.getInt("stat-strength", stats.getStatStrength()));
        stats.setStatDefense(section.getInt("stat-defense", stats.getStatDefense()));
        stats.setStatMovementSpeed(section.getInt("stat-move-speed", stats.getStatMovementSpeed()));
        stats.setStatMiningSpeed(section.getInt("stat-mine-speed", stats.getStatMiningSpeed()));
        stats.setStatWoodcuttingSpeed(section.getInt("stat-wood-speed", stats.getStatWoodcuttingSpeed()));
        stats.setStatHealthRegen(section.getInt("stat-health-regen", stats.getStatHealthRegen()));

        stats.setStatStaminaMax(section.getInt("stat-stamina-max", stats.getStatStaminaMax()));
        stats.setStatStaminaRegen(section.getInt("stat-stamina-regen", stats.getStatStaminaRegen()));
        stats.setCurrentStamina(section.getDouble("current-stamina", stats.getCurrentStamina()));
        stats.setCurrentHealth(section.getDouble("current-health", stats.getCurrentHealth()));

        ConfigurationSection abilitySection = config.getConfigurationSection("rpg.abilities");
        if (abilitySection != null) {
            for (String key : abilitySection.getKeys(false)) {
                try {
                    AbilityType type = AbilityType.valueOf(key.toUpperCase());
                    ConfigurationSection skillData = abilitySection.getConfigurationSection(key);
                    if (skillData != null) {
                        stats.setAbilityLevel(type, skillData.getInt("level", 0));
                        stats.setAbilityExp(type, skillData.getLong("exp", 0));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Habilidad RPG desconocida en YAML: " + key);
                }
            }
        }

        ConfigurationSection skillSection = config.getConfigurationSection("rpg.skills.levels");
        if (skillSection != null) {
            for (String key : skillSection.getKeys(false)) {
                stats.setLevel(key, skillSection.getInt(key, 0));
            }
        }
        ConfigurationSection expSection = config.getConfigurationSection("rpg.skills.experience");
        if (expSection != null) {
            for (String key : expSection.getKeys(false)) {
                stats.setExperience(key, expSection.getLong(key, 0L));
            }
        }
    }

    private void saveRPGStats(YamlConfiguration config, RPGStats stats) {
        ConfigurationSection statsSection = config.getConfigurationSection("rpg.stats");
        if (statsSection == null) statsSection = config.createSection("rpg.stats");

        statsSection.set("level", stats.getLevel());
        statsSection.set("total-exp", stats.getTotalExperience());
        statsSection.set("unspent-points", stats.getUnspentPoints());
        statsSection.set("max-vanilla-level", stats.getMaxVanillaLevelReached());

        statsSection.set("stat-health", stats.getStatHealth());
        statsSection.set("stat-strength", stats.getStatStrength());
        statsSection.set("stat-defense", stats.getStatDefense());
        statsSection.set("stat-move-speed", stats.getStatMovementSpeed());
        statsSection.set("stat-mine-speed", stats.getStatMiningSpeed());
        statsSection.set("stat-wood-speed", stats.getStatWoodcuttingSpeed());
        statsSection.set("stat-health-regen", stats.getStatHealthRegen());

        statsSection.set("stat-stamina-max", stats.getStatStaminaMax());
        statsSection.set("stat-stamina-regen", stats.getStatStaminaRegen());
        statsSection.set("current-stamina", stats.getCurrentStamina());
        statsSection.set("current-health", stats.getCurrentHealth());

        ConfigurationSection abilitySection = config.getConfigurationSection("rpg.abilities");
        if (abilitySection == null) abilitySection = config.createSection("rpg.abilities");
        for (AbilityType type : AbilityType.values()) {
            ConfigurationSection skillData = abilitySection.getConfigurationSection(type.name().toLowerCase());
            if (skillData == null) skillData = abilitySection.createSection(type.name().toLowerCase());
            skillData.set("level", stats.getAbilityLevel(type));
            skillData.set("exp", stats.getAbilityExp(type));
        }

        config.set("rpg.skills.levels", stats.getSkillLevels());
        config.set("rpg.skills.experience", stats.getSkillExperience());
    }

    // --- INICIALIZACIÓN ---

    private void initializeNewPlayer(YamlConfiguration config, UUID uuid, String username) {
        config.set("player.uuid", uuid.toString());
        config.set("player.name", username);
        config.set("player.first-join", System.currentTimeMillis());
        config.set("player.last-login", System.currentTimeMillis());
        config.set("player.last-known-name", username);

        config.set("auth.registered", false);
        config.set("auth.password-hash", null);
        config.set("auth.failed-attempts", 0);
        config.set("auth.locked-until", 0);
        config.set("auth.is-logged", false);

        config.set("economy.safe-balance", plugin.getConfig().getDouble("economy.starting-balance", 100.0));
        config.set("economy.cash-balance", 0.0);
        config.set("economy.total-earned", 0.0);
        config.set("economy.total-spent", 0.0);

        config.set("roles.current-role", Role.PLAYER.name());
        config.set("roles.role-history", new ArrayList<>());

        config.set("security.is-banned", false);
        config.set("security.ban-reason", "");
        config.set("security.banned-by", "");
        config.set("security.ban-time", 0);
        config.set("security.warnings", 0);

        RPGStats newStats = new RPGStats(uuid, plugin);
        saveRPGStats(config, newStats);
    }

    // --- ROLES ---

    public void setPlayerRole(UUID uuid, Role role) {
        YamlConfiguration config = getPlayerConfig(uuid);

        List<Map<String, Object>> history = new ArrayList<>();
        List<?> rawRoleHistory = config.getList("roles.role-history", new ArrayList<>());
        for (Object obj : rawRoleHistory) {
            if (obj instanceof Map<?, ?> rawMap) {
                Map<String, Object> converted = new HashMap<>();
                rawMap.forEach((k, v) -> { if (k instanceof String) converted.put((String) k, v); });
                if (converted.containsKey("to") && (long) converted.get("to") == 0L) {
                    converted.put("to", System.currentTimeMillis());
                }
                history.add(converted);
            }
        }

        Map<String, Object> roleEntry = new HashMap<>();
        roleEntry.put("role", role.name());
        roleEntry.put("from", System.currentTimeMillis());
        roleEntry.put("to", 0L);
        history.add(roleEntry);

        config.set("roles.current-role", role.name());
        config.set("roles.role-history", history);
        savePlayerConfig(uuid);
    }

    // --- UTILS ---

    public String getPlayerName(UUID uuid) {
        return getPlayerConfig(uuid).getString("player.last-known-name", "Unknown");
    }

    public CompletableFuture<UUID> getUUIDFromUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = Bukkit.getPlayerExact(username);
            if (player != null) return player.getUniqueId();

            File[] files = userDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    if (username.equalsIgnoreCase(cfg.getString("player.last-known-name"))) {
                        return UUID.fromString(file.getName().replace(".yml", ""));
                    }
                }
            }
            return null;
        });
    }

    public boolean isRegistered(UUID uuid) {
        return getPlayerConfig(uuid).getBoolean("auth.registered", false);
    }

    public boolean playerFileExists(UUID uuid) {
        return new File(userDataFolder, uuid.toString() + ".yml").exists();
    }

    public void saveAll() {
        for (UUID uuid : cachedConfigs.keySet()) {
            savePlayerConfig(uuid);
        }
    }
}
