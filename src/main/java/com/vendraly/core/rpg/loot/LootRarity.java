package com.vendraly.core.rpg.loot;

/**
 * Rarezas posibles de un botín.
 */
public enum LootRarity {
    COMMON(1.0),
    UNCOMMON(0.5),
    RARE(0.25),
    EPIC(0.1),
    LEGENDARY(0.05);

    private final double weight;

    LootRarity(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
