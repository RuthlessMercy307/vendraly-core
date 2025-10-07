package com.vendraly.core.database;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.roles.Role;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.ability.AbilityType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.UUID;
import java.io.File;
import java.io.IOException;
import java.util.*;

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

    /**
     * Obtiene o crea la configuración YAML para un jugador.
     */
    public YamlConfiguration getPlayerConfig(UUID uuid) {
        if (cachedConfigs.containsKey(uuid)) {
            return cachedConfigs.get(uuid);
        }

        File playerFile = new File(userDataFolder, uuid.toString() + ".yml");
        // Aseguramos que el archivo existe o será creado.
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

    /**
     * Guarda la configuración de un jugador en el disco.
     */
    public void savePlayerConfig(UUID uuid) {
        YamlConfiguration config = cachedConfigs.get(uuid);
        if (config == null) return; // No hay config en caché para guardar.

        File playerFile = new File(userDataFolder, uuid.toString() + ".yml");

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de " + uuid + ": " + e.getMessage());
        }
    }

    // --- LÓGICA DE CARGA Y GUARDADO DEL MODELO (SINGLE SOURCE OF TRUTH) ---

    /**
     * Carga todos los datos de un jugador (Auth, Economy, RPGStats) desde el disco.
     * Siempre devuelve una instancia fresca de PlayerData.
     * @param uuid UUID del jugador.
     * @param username Nombre del jugador (usado para el constructor).
     */
    public PlayerData loadPlayerData(UUID uuid, String username) {
        YamlConfiguration config = getPlayerConfig(uuid);
        PlayerData data = new PlayerData(uuid, username);

        // Si es la primera vez que se une, inicializa los valores por defecto y guarda.
        if (!config.contains("player.first-join")) {
            // Inicializamos el YAML y luego recargamos los valores en el objeto 'data'
            initializeNewPlayer(config, uuid, username);
        }

        // Cargar datos de autenticación
        data.setRegistered(config.getBoolean("auth.registered", false));
        data.setPasswordHash(config.getString("auth.password-hash", null));
        data.setFailedAttempts(config.getInt("auth.failed-attempts", 0));
        data.setLockedUntil(config.getLong("auth.locked-until", 0));
        data.setLoggedIn(config.getBoolean("auth.is-logged", false));

        // Cargar economía
        // CORRECCIÓN: Usamos el método getDouble con valor por defecto del plugin
        data.setBalance(config.getDouble("economy.safe-balance", plugin.getConfig().getDouble("economy.starting-balance", 100.0)));
        data.setCashBalance(config.getDouble("economy.cash-balance", 0.0));

        // Cargar rol
        String roleName = config.getString("roles.current-role", "PLAYER");
        try {
            data.setCurrentRole(Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            data.setCurrentRole(Role.PLAYER);
        }

        // --- LÓGICA RPG ---
        // El constructor de PlayerData ya inicializa RPGStats. Lo cargamos en ese objeto.
        loadRPGStats(config, data.getRpgStats());
        // ------------------

        // Actualizar último login y nombre
        config.set("player.last-login", System.currentTimeMillis());
        config.set("player.last-known-name", username);

        savePlayerConfig(uuid); // Guarda los cambios de nombre/login o inicialización.

        return data;
    }

    /**
     * Guarda todos los datos del PlayerData al disco.
     */
    public void savePlayerData(PlayerData data) {
        UUID uuid = data.getPlayerUUID();
        YamlConfiguration config = getPlayerConfig(uuid);

        // Datos del jugador
        config.set("player.name", data.getPlayerName());
        config.set("player.last-known-name", data.getPlayerName());
        config.set("player.last-updated", System.currentTimeMillis());

        // Datos de autenticación
        config.set("auth.registered", data.isRegistered());
        config.set("auth.password-hash", data.getPasswordHash());
        config.set("auth.failed-attempts", data.getFailedAttempts());
        config.set("auth.locked-until", data.getLockedUntil());
        config.set("auth.is-logged", data.isLoggedIn());

        // Datos de economía
        config.set("economy.safe-balance", data.getBalance());
        config.set("economy.cash-balance", data.getCashBalance());

        // Datos de rol
        config.set("roles.current-role", data.getCurrentRole().name());

        // --- LÓGICA RPG ---
        saveRPGStats(config, data.getRpgStats());
        // ------------------

        savePlayerConfig(uuid);
    }

    /**
     * Limpia la caché de un jugador (usar cuando se desconecta)
     */
    public void unloadPlayer(UUID uuid) {
        // En este gestor, solo necesitamos guardar la config YAML y limpiar el caché ligero.
        savePlayerConfig(uuid);
        cachedConfigs.remove(uuid);
    }

    // --- LÓGICA RPG ESPECÍFICA ---

    /**
     * Carga la clase RPGStats desde la configuración YAML en un objeto PlayerData existente.
     * CORRECCIÓN: Ahora acepta el objeto RPGStats para que lo llene, no lo crea.
     */
    private void loadRPGStats(YamlConfiguration config, RPGStats stats) {
        ConfigurationSection section = config.getConfigurationSection("rpg.stats");

        if (section == null) {
            // El objeto 'stats' ya fue inicializado por el constructor de PlayerData,
            // no hacemos nada si la sección no existe.
            return;
        }

        // Progresión y Puntos
        stats.setLevel(section.getInt("level", stats.getLevel()));
        stats.setTotalExperience(section.getLong("total-exp", stats.getTotalExperience()));
        stats.setUnspentPoints(section.getInt("unspent-points", stats.getUnspentPoints()));
        stats.setMaxVanillaLevelReached(section.getInt("max-vanilla-level", stats.getMaxVanillaLevelReached()));

        // Estadísticas Invertibles
        stats.setStatHealth(section.getInt("stat-health", stats.getStatHealth()));
        stats.setStatStrength(section.getInt("stat-strength", stats.getStatStrength()));
        stats.setStatDefense(section.getInt("stat-defense", stats.getStatDefense()));
        stats.setStatMovementSpeed(section.getInt("stat-move-speed", stats.getStatMovementSpeed()));
        stats.setStatMiningSpeed(section.getInt("stat-mine-speed", stats.getStatMiningSpeed()));
        stats.setStatWoodcuttingSpeed(section.getInt("stat-wood-speed", stats.getStatWoodcuttingSpeed()));
        stats.setStatHealthRegen(section.getInt("stat-health-regen", stats.getStatHealthRegen()));

        // Estadísticas de Estamina y Vida Actual
        // Usamos los valores base de RPGStats como valor por defecto si no existen en YAML
        stats.setStatStaminaMax(section.getInt("stat-stamina-max", stats.getStatStaminaMax()));
        stats.setStatStaminaRegen(section.getInt("stat-stamina-regen", stats.getStatStaminaRegen()));
        stats.setCurrentStamina(section.getDouble("current-stamina", stats.getCurrentStamina()));
        stats.setCurrentHealth(section.getDouble("current-health", stats.getCurrentHealth()));

        // Cargar datos de habilidades (usando AbilityType)
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

        // Cargar habilidades secundarias directas
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

    /**
     * Guarda la clase RPGStats en la configuración YAML.
     */
    private void saveRPGStats(YamlConfiguration config, RPGStats stats) {
        ConfigurationSection statsSection = config.createSection("rpg.stats");

        // Progresión y Puntos
        statsSection.set("level", stats.getLevel());
        statsSection.set("total-exp", stats.getTotalExperience());
        statsSection.set("unspent-points", stats.getUnspentPoints());
        statsSection.set("max-vanilla-level", stats.getMaxVanillaLevelReached());

        // Estadísticas Invertibles
        statsSection.set("stat-health", stats.getStatHealth());
        statsSection.set("stat-strength", stats.getStatStrength());
        statsSection.set("stat-defense", stats.getStatDefense());
        statsSection.set("stat-move-speed", stats.getStatMovementSpeed());
        statsSection.set("stat-mine-speed", stats.getStatMiningSpeed());
        statsSection.set("stat-wood-speed", stats.getStatWoodcuttingSpeed());
        statsSection.set("stat-health-regen", stats.getStatHealthRegen());

        // Estadísticas de Estamina y Vida Actual
        statsSection.set("stat-stamina-max", stats.getStatStaminaMax());
        statsSection.set("stat-stamina-regen", stats.getStatStaminaRegen());
        statsSection.set("current-stamina", stats.getCurrentStamina());
        statsSection.set("current-health", stats.getCurrentHealth());

        // Guardar datos de habilidades (usando AbilityType)
        ConfigurationSection abilitySection = config.createSection("rpg.abilities");
        for (AbilityType type : AbilityType.values()) {
            ConfigurationSection skillData = abilitySection.createSection(type.name().toLowerCase());
            skillData.set("level", stats.getAbilityLevel(type));
            skillData.set("exp", stats.getAbilityExp(type));
        }

        // CORRECCIÓN CRÍTICA: Guardar Map<String, ?> directamente.
        config.set("rpg.skills.levels", stats.getSkillLevels());
        config.set("rpg.skills.experience", stats.getSkillExperience());
    }

    // --- LÓGICA DE INICIALIZACIÓN ---

    private void initializeNewPlayer(YamlConfiguration config, UUID uuid, String username) {
        // ... (Tu código de inicialización existente) ...
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

        // --- INICIALIZACIÓN RPG ---
        RPGStats newStats = new RPGStats(uuid);
        saveRPGStats(config, newStats);
    }

    // --- MÉTODOS DE CONVENIENCIA (PARA JUGADORES OFFLINE) ---

    // La mayoría de los métodos de conveniencia (get/set/modify Balance, get/set Authenticated, etc.)
    // han sido ELIMINADOS, ya que su uso es inseguro cuando el jugador está ONLINE.

    /**
     * Establece el rol de un jugador
     */
    public void setPlayerRole(UUID uuid, Role role) {
        YamlConfiguration config = getPlayerConfig(uuid);
        config.set("roles.current-role", role.name());

        // ... (Tu lógica de historial de roles) ...
        List<?> rawRoleHistory = config.getList("roles.role-history", new ArrayList<>());
        List<Map<String, Object>> roleHistory = new ArrayList<>();

        for (Object item : rawRoleHistory) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> convertedMap = new HashMap<>();
                rawMap.forEach((k, v) -> {
                    if (k instanceof String) convertedMap.put((String) k, v);
                });

                if (convertedMap.containsKey("to") && convertedMap.get("to") instanceof Long && (Long)convertedMap.get("to") == 0L) {
                    convertedMap.put("to", System.currentTimeMillis());
                }
                roleHistory.add(convertedMap);
            }
        }

        Map<String, Object> roleEntry = new HashMap<>();
        roleEntry.put("role", role.name());
        roleEntry.put("from", System.currentTimeMillis());
        roleEntry.put("to", 0L);
        roleHistory.add(roleEntry);

        config.set("roles.role-history", roleHistory);
        savePlayerConfig(uuid);
    }

    /**
     * Obtiene el nombre del jugador desde los datos (principalmente para offline)
     */
    public String getPlayerName(UUID uuid) {
        YamlConfiguration config = getPlayerConfig(uuid);
        return config.getString("player.last-known-name", "Unknown");
    }

    /**
     * CORRECCIÓN: Método para obtener UUID por Nombre (Asíncrono - necesario para /vban).
     * Esto necesita una implementación que consulte la base de datos o todos los archivos YAML.
     * Dado que no tengo acceso a tu DB, asumo que buscarás en el disco, lo cual es MUY lento.
     */
    public CompletableFuture<UUID> getUUIDFromUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            // Chequeo rápido si el jugador está online
            Player player = Bukkit.getPlayerExact(username);
            if (player != null) {
                return player.getUniqueId();
            }

            // CORRECCIÓN: Búsqueda lenta en el disco (solo si es YAML)
            // Esto es ineficiente y debe ser reemplazado por una DB SQL/MongoDB.
            File[] files = userDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    if (username.equalsIgnoreCase(config.getString("player.last-known-name"))) {
                        // El nombre coincide, extraemos el UUID del nombre de archivo.
                        return UUID.fromString(file.getName().replace(".yml", ""));
                    }
                }
            }
            return null;
        });
    }

    // --- MÉTODOS DE VERIFICACIÓN (Necesarios para jugadores offline) ---

    public boolean isRegistered(UUID uuid) {
        YamlConfiguration config = getPlayerConfig(uuid);
        return config.getBoolean("auth.registered", false);
    }

    public boolean playerFileExists(UUID uuid) {
        return new File(userDataFolder, uuid.toString() + ".yml").exists();
    }

    // --- MANEJO DE CIERRE ---

    /**
     * Guarda todos los datos en caché (usar en onDisable).
     */
    public void saveAll() {
        // Guardar todos los YAML que aún estén en caché
        for (UUID uuid : cachedConfigs.keySet()) {
            savePlayerConfig(uuid);
        }
    }
}