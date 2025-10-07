package com.vendraly.core.rpg.stats;

/**
 * Enum que define los tipos de atributos principales de RPG.
 * Cada uno tiene un nombre visible para mostrar en menús/lore.
 */
public enum StatType {

    HEALTH_MAX("Vida Máxima"),
    STRENGTH("Fuerza"),
    DEFENSE("Defensa"),
    MOVEMENT_SPEED("Velocidad Movimiento"),
    MINING_SPEED("Velocidad Minado"),
    WOODCUTTING_SPEED("Velocidad Talado"),
    HEALTH_REGEN("Regeneración de Vida"),
    STAMINA_MAX("Estamina Máxima"),
    STAMINA_REGEN("Regeneración de Estamina");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    /** Nombre legible que se usa en menús, lore, botones, etc. */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte un nombre mostrado en el menú a un StatType.
     * Ejemplo: "Fuerza" -> StatType.STRENGTH
     */
    public static StatType fromDisplayName(String name) {
        for (StatType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null; // Si no existe, devolvemos null
    }
}
