package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Gestiona el nivel base de los mobs y las zonas de spawn especiales.
 * El nivel base global escala con el tiempo real para asegurar la dificultad.
 */
public class WorldDifficultyManager {

    private final Main plugin;
    private final List<SpawnZone> spawnZones = new ArrayList<>(); // Lista de zonas de spawn fijas

    // Variables de Dificultad Global
    private int globalBaseLevel = 1;
    private int serverDays = 0; // Días transcurridos en el servidor (24h reales)

    // Constantes de Escalado
    // AJUSTE: El nivel base global sube 1 cada 1 día real.
    private static final int DAYS_PER_LEVEL_INCREASE = 1;
    private static final int MAX_GLOBAL_LEVEL = 100;

    // Variación de Nivel: Permite que los mobs Globales tengan hasta este rango por encima del nivel base.
    private static final int LEVEL_VARIANCE = 5;
    // Por ejemplo: Nivel Base 1 genera mobs de Nivel 1 a 6.

    public WorldDifficultyManager(Main plugin) {
        this.plugin = plugin;
        loadConfiguration();
        startDailyChecker();
    }

    // ===============================================
    // LÓGICA DE ESCALADO GLOBAL
    // ===============================================

    /**
     * Tarea programada que verifica los días transcurridos y escala el nivel base.
     * Se ejecuta una vez cada día (24 horas reales).
     */
    private void startDailyChecker() {
        // Ticks para 24 horas (20 * 60 * 60 * 24 = 1,728,000 ticks)
        final long TICKS_PER_DAY = 20L * 60L * 60L * 24L;

        new BukkitRunnable() {
            @Override
            public void run() {
                // 1. Aumentar días del servidor (Solo para registro)
                serverDays++;

                // 2. Aplicar el escalado (cada 1 día real)
                if (serverDays % DAYS_PER_LEVEL_INCREASE == 0 && globalBaseLevel < MAX_GLOBAL_LEVEL) {
                    globalBaseLevel++;
                    // Usamos Bukkit.broadcastMessage para notificar a los jugadores sobre el cambio de dificultad
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "[DIOSES RPG] La dificultad global ha aumentado." +
                            ChatColor.RED + " Nivel base: " + globalBaseLevel);
                }

                // 3. Guardar el estado
                saveConfiguration();
            }
        }.runTaskTimerAsynchronously(plugin, TICKS_PER_DAY, TICKS_PER_DAY);
    }

    /**
     * Calcula el nivel de spawn para una ubicación dada.
     * @param location Ubicación del spawn.
     * @return El nivel RPG calculado.
     */
    public int getSpawnLevel(Location location) {
        // 1. Revisar si está en una SpawnZone (Nivel Fijo)
        for (SpawnZone zone : spawnZones) {
            if (zone.contains(location)) {
                // El nivel de la zona ignora la dificultad global
                return ThreadLocalRandom.current().nextInt(zone.getMinLevel(), zone.getMaxLevel() + 1);
            }
        }

        // 2. Cálculo para la Dificultad Global (Mobs Globales)

        // Nivel Mínimo Global (al menos 1)
        int minLvl = Math.max(1, globalBaseLevel);

        // Nivel Máximo Global = Nivel Base + Variación (ej. +5)
        int maxLvl = globalBaseLevel + LEVEL_VARIANCE;

        // Aseguramos que los niveles no excedan el máximo absoluto
        maxLvl = Math.min(maxLvl, MAX_GLOBAL_LEVEL);
        minLvl = Math.min(minLvl, maxLvl);

        // Generar un nivel aleatorio en el rango [minLvl, maxLvl]
        return ThreadLocalRandom.current().nextInt(minLvl, maxLvl + 1);
    }

    // ===============================================
    // GESTIÓN DE ZONAS (CONFIGURACIÓN YAML)
    // ===============================================

    private void loadConfiguration() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.globalBaseLevel = config.getInt("world-difficulty.global-base-level", 1);
        this.serverDays = config.getInt("world-difficulty.server-days", 0);
        this.spawnZones.clear();

        ConfigurationSection zonesSection = config.getConfigurationSection("spawn-zones");
        if (zonesSection == null) return;

        for (String key : zonesSection.getKeys(false)) {
            ConfigurationSection zoneConfig = zonesSection.getConfigurationSection(key);
            if (zoneConfig == null) continue;

            try {
                UUID worldUID = UUID.fromString(zoneConfig.getString("world-uuid"));
                World world = Bukkit.getWorld(worldUID);
                if (world == null) {
                    plugin.getLogger().warning("Mundo no encontrado para la zona: " + key + ". UUID: " + worldUID);
                    continue;
                }

                int minLvl = zoneConfig.getInt("min-level");
                int maxLvl = zoneConfig.getInt("max-level");
                int maxMobs = zoneConfig.getInt("max-mobs", 10);
                int delayTicks = zoneConfig.getInt("spawn-delay-ticks", 200);

                List<String> mobNames = zoneConfig.getStringList("mob-types");
                List<EntityType> mobTypes = mobNames.stream()
                        .map(name -> {
                            try { return EntityType.valueOf(name.toUpperCase()); }
                            catch (IllegalArgumentException e) { return null; }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (mobTypes.isEmpty()) {
                    plugin.getLogger().warning("Zona '" + key + "' no tiene mobs válidos definidos. Saltando carga.");
                    continue;
                }

                Location p1 = new Location(world, zoneConfig.getInt("x1"), zoneConfig.getInt("y1"), zoneConfig.getInt("z1"));
                Location p2 = new Location(world, zoneConfig.getInt("x2"), zoneConfig.getInt("y2"), zoneConfig.getInt("z2"));

                SpawnZone zone = new SpawnZone(key, world, minLvl, maxLvl, p1, p2, mobTypes, maxMobs, delayTicks);
                spawnZones.add(zone);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error cargando zona '" + key + "'. Causa: " + e.getMessage());
            }
        }
    }

    public void saveConfiguration() {
        FileConfiguration config = plugin.getConfig();

        config.set("world-difficulty.global-base-level", this.globalBaseLevel);
        config.set("world-difficulty.server-days", this.serverDays);

        // Limpiamos la sección anterior antes de guardar las zonas actuales
        config.set("spawn-zones", null);

        for (SpawnZone zone : spawnZones) {
            String path = "spawn-zones." + zone.getName();

            // Guardar datos básicos y niveles
            config.set(path + ".world-uuid", zone.getWorldUID().toString());
            config.set(path + ".min-level", zone.getMinLevel());
            config.set(path + ".max-level", zone.getMaxLevel());

            // Guardar Coordenadas
            config.set(path + ".x1", zone.getX1());
            config.set(path + ".y1", zone.getY1());
            config.set(path + ".z1", zone.getZ1());
            config.set(path + ".x2", zone.getX2());
            config.set(path + ".y2", zone.getY2());
            config.set(path + ".z2", zone.getZ2());

            // Guardar CAMPOS DE SPAWNER
            config.set(path + ".max-mobs", zone.getMaxMobs());
            config.set(path + ".spawn-delay-ticks", zone.getSpawnDelayTicks());

            // Guardar la lista de MobTypes como Strings
            List<String> mobNames = zone.getMobTypes().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            config.set(path + ".mob-types", mobNames);
        }

        plugin.saveConfig();
    }

    // ===============================================
    // MÉTODOS DE ADMINISTRACIÓN Y GETTERS
    // ===============================================

    public void addZone(SpawnZone zone) {
        // Eliminar duplicados por nombre antes de añadir el nuevo
        spawnZones.removeIf(z -> z.getName().equalsIgnoreCase(zone.getName()));
        this.spawnZones.add(zone);
        saveConfiguration();
    }

    public boolean removeZone(String name) {
        boolean removed = spawnZones.removeIf(z -> z.getName().equalsIgnoreCase(name));
        if (removed) {
            saveConfiguration();
        }
        return removed;
    }

    // Getter necesario para ZoneSpawner
    public List<SpawnZone> getSafeZones() {
        return Collections.unmodifiableList(spawnZones);
    }
}