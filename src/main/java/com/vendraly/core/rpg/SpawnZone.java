package com.vendraly.core.rpg;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import java.util.List;
import java.util.UUID;

/**
 * Representa una zona rectangular de spawn de mobs con niveles específicos.
 * Define el área (Bounding Box) y las reglas de spawn para los monstruos RPG.
 */
public class SpawnZone {

    private final String name;
    private final UUID worldUID;
    private final int minLevel;
    private final int maxLevel;

    // Configuración del Spawner
    private final List<EntityType> mobTypes; // Tipos de mobs permitidos
    private final int maxMobs; // Cantidad máxima de mobs en la zona
    private final int spawnDelayTicks; // Retraso entre intentos de spawn (en ticks)

    // Coordenadas del Bounding Box (siempre min/max)
    private final int x1, y1, z1;
    private final int x2, y2, z2;

    /**
     * Constructor para la SpawnZone.
     * @param name Nombre único de la zona.
     * @param world Mundo de la zona.
     * @param minLevel Nivel mínimo de los mobs.
     * @param maxLevel Nivel máximo de los mobs.
     * @param p1 Esquina 1 (cualquiera).
     * @param p2 Esquina 2 (cualquiera).
     * @param mobTypes Lista de tipos de mobs que pueden aparecer.
     * @param maxMobs Límite de mobs en la zona.
     * @param spawnDelayTicks Ticks entre intentos de spawn.
     */
    public SpawnZone(String name, World world, int minLevel, int maxLevel,
                     Location p1, Location p2, List<EntityType> mobTypes,
                     int maxMobs, int spawnDelayTicks) {

        this.name = name;
        this.worldUID = world.getUID();
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;

        // Asignación de Spawner/Mob Types
        this.mobTypes = mobTypes;
        this.maxMobs = maxMobs;
        this.spawnDelayTicks = spawnDelayTicks;

        // ALMACENAMIENTO ROBUSTO: Garantiza que (x1, y1, z1) sea siempre la esquina mínima
        // y (x2, y2, z2) sea siempre la esquina máxima.
        this.x1 = Math.min(p1.getBlockX(), p2.getBlockX());
        this.y1 = Math.min(p1.getBlockY(), p2.getBlockY());
        this.z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
        this.x2 = Math.max(p1.getBlockX(), p2.getBlockX());
        this.y2 = Math.max(p1.getBlockY(), p2.getBlockY());
        this.z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());
    }

    /**
     * Verifica si una ubicación está dentro de la zona.
     * Requiere que el mundo coincida y que las coordenadas estén dentro del Bounding Box.
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getUID().equals(worldUID)) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Verificación de rango (Bounding Box)
        return x >= x1 && x <= x2 &&
                y >= y1 && y <= y2 &&
                z >= z1 && z <= z2;
    }

    // =========================================================
    // GETTERS PÚBLICOS
    // =========================================================

    // Getters de datos básicos
    public String getName() { return name; }
    public UUID getWorldUID() { return worldUID; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }

    // Getters de Spawner
    public List<EntityType> getMobTypes() { return mobTypes; }
    public int getMaxMobs() { return maxMobs; }
    public int getSpawnDelayTicks() { return spawnDelayTicks; }

    // Getters de coordenadas (Bounding Box)
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }
}