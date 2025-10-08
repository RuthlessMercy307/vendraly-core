package com.vendraly.core.rpg.stats;

/**
 * Tipos de estadísticas principales del sistema RPG.
 */
public enum StatType {
    HEALTH(20.0),
    STAMINA(100.0),
    STRENGTH(5.0),
    AGILITY(5.0),
    DEFENSE(5.0),
    MAGIC(5.0),
    RESILIENCE(5.0),
    LUCK(1.0);

    private final double baseValue;

    StatType(double baseValue) {
        this.baseValue = baseValue;
    }

    public double getBaseValue() {
        return baseValue;
    }
}
