package com.vendraly.core.rpg.stats;

import java.util.EnumMap;
import java.util.Map;

/**
 * Contenedor mutable de estadísticas RPG.
 */
public class RPGStats {

    private final Map<StatType, Double> stats = new EnumMap<>(StatType.class);

    public RPGStats() {
        for (StatType type : StatType.values()) {
            stats.put(type, type.getBaseValue());
        }
    }

    public double getStat(StatType type) {
        return stats.getOrDefault(type, type.getBaseValue());
    }

    public void setStat(StatType type, double value) {
        stats.put(type, value);
    }

    public void addStat(StatType type, double value) {
        stats.merge(type, value, Double::sum);
    }

    public Map<StatType, Double> asMap() {
        return stats;
    }
}
