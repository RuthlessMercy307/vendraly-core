package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gestiona el spawn periódico de mobs en las SpawnZones definidas.
 * Respeta el límite máximo de mobs por zona y spawnea en el suelo.
 */
public class ZoneSpawner {

    private final Main plugin;
    private final WorldDifficultyManager difficultyManager;
    private BukkitTask taskHandler;

    // Configuración desde config.yml
    private final int initialDelay;
    private final long intervalTicks;
    private final int maxAttempts;
    private final boolean logSpawns;

    // Mapa para rastrear el contador de mobs por zona.
    private final Map<String, Integer> currentMobCount = Collections.synchronizedMap(new HashMap<>());

    public ZoneSpawner(Main plugin) {
        this.plugin = plugin;
        this.difficultyManager = plugin.getDifficultyManager();

        // Cargar valores desde config.yml
        this.initialDelay = plugin.getConfig().getInt("zone-spawner.initial-delay", 60);
        this.intervalTicks = plugin.getConfig().getLong("zone-spawner.interval-ticks", 200L);
        this.maxAttempts = plugin.getConfig().getInt("zone-spawner.max-attempts", 15);
        this.logSpawns = plugin.getConfig().getBoolean("zone-spawner.log-spawns", true);
    }

    /**
     * Inicia la tarea de spawn periódico.
     */
    public void startSpawnerTask() {
        if (taskHandler != null) {
            taskHandler.cancel();
        }

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                currentMobCount.clear();
                recalculateMobPopulation();

                for (SpawnZone zone : difficultyManager.getSafeZones()) {
                    attemptSpawnInZone(zone);
                }
            }
        };

        this.taskHandler = runnable.runTaskTimer(plugin, initialDelay, intervalTicks);

        if (logSpawns) {
            plugin.getLogger().info("[ZoneSpawner] Tarea de spawn iniciada con intervalo " + intervalTicks + " ticks.");
        }
    }

    /**
     * Detiene la tarea de spawn.
     */
    public void stopSpawnerTask() {
        if (taskHandler != null) {
            taskHandler.cancel();
            taskHandler = null;
        }
        currentMobCount.clear();

        if (logSpawns) {
            plugin.getLogger().info("[ZoneSpawner] Tarea de spawn detenida.");
        }
    }

    /**
     * Recalcula cuántos mobs de la lista de mobs vivos están actualmente en cada zona.
     */
    private void recalculateMobPopulation() {
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity.getType().isAlive() && entity.getType() != EntityType.PLAYER) {
                    Location loc = entity.getLocation();
                    for (SpawnZone zone : difficultyManager.getSafeZones()) {
                        if (zone.contains(loc)) {
                            currentMobCount.merge(zone.getName(), 1, Integer::sum);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Intenta spawnear un mob en una zona si no se ha alcanzado el límite.
     */
    private void attemptSpawnInZone(SpawnZone zone) {
        String zoneName = zone.getName();
        int currentCount = currentMobCount.getOrDefault(zoneName, 0);

        if (currentCount >= zone.getMaxMobs()) {
            return;
        }

        Location spawnLoc = findSafeSpawnLocation(zone);
        if (spawnLoc == null) return;

        List<EntityType> mobs = zone.getMobTypes();
        if (mobs.isEmpty()) return;

        EntityType mobType = mobs.get(ThreadLocalRandom.current().nextInt(mobs.size()));
        World world = spawnLoc.getWorld();
        if (world == null) return;

        LivingEntity mob = (LivingEntity) world.spawnEntity(spawnLoc, mobType);
        currentMobCount.put(zoneName, currentCount + 1);

        // Obtener nivel de la zona y aplicar atributos RPG al mob
        int level = difficultyManager.getSpawnLevel(spawnLoc);
        plugin.getMonsterListener().applyRpgAttributesPublic(mob, level);

        if (logSpawns) {
            plugin.getLogger().info("[ZoneSpawner] Spawned " + mobType.name()
                    + " (Lv." + level + ") in zone: " + zoneName);
        }
    }

    /**
     * Busca un punto aleatorio dentro de la zona que esté en el suelo.
     */
    private Location findSafeSpawnLocation(SpawnZone zone) {
        World world = plugin.getServer().getWorld(zone.getWorldUID());
        if (world == null) return null;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(zone.getX1(), zone.getX2() + 1);
            int z = random.nextInt(zone.getZ1(), zone.getZ2() + 1);

            for (int y = zone.getY2(); y >= zone.getY1(); y--) {
                Location currentLoc = new Location(world, x, y, z);
                Block block = currentLoc.getBlock();

                if (block.getType().isSolid()) continue;
                Block blockBelow = currentLoc.clone().subtract(0, 1, 0).getBlock();
                if (!blockBelow.getType().isSolid()) continue;
                Block blockAbove = currentLoc.clone().add(0, 1, 0).getBlock();
                if (blockAbove.getType().isSolid()) continue;

                return currentLoc.add(0.5, 0, 0.5);
            }
        }
        return null;
    }
}
