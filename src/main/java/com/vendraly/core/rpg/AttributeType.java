package com.vendraly.core.rpg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Define los tipos de atributos RPG que pueden tener las armas y armaduras.
 * Utiliza un nombre legible (displayName) y un color asociado
 * para la presentación en el juego (Lore, Menús).
 */
public enum AttributeType {
    // --- ATRIBUTOS DE COMBATE ---
    STRENGTH("Fuerza", NamedTextColor.RED),
    AGILITY("Agilidad", NamedTextColor.YELLOW),
    DEFENSE("Defensa", NamedTextColor.BLUE),            // Defensa física
    ATTACK_SPEED("Velocidad de Ataque", NamedTextColor.AQUA),
    ATTACK_RANGE("Alcance", NamedTextColor.LIGHT_PURPLE),
    CRITICAL_CHANCE("Probabilidad Crítica", NamedTextColor.GOLD), // Nuevo

    // --- ATRIBUTOS DE VIDA Y ENERGÍA ---
    HEALTH("Vida Máxima", NamedTextColor.DARK_RED),
    STAMINA_MAX("Estamina Máxima", NamedTextColor.GREEN),
    HEALTH_REGEN("Regeneración de Vida", NamedTextColor.DARK_GREEN),
    MOVEMENT_SPEED("Velocidad de Movimiento", NamedTextColor.WHITE), // Nuevo

    // --- ATRIBUTOS DE PROFESIONES ---
    MINING_SPEED("Velocidad de Minería", NamedTextColor.GRAY),       // Nuevo
    WOODCUTTING_SPEED("Velocidad de Tala", NamedTextColor.DARK_GREEN), // Nuevo

    // --- ATRIBUTOS DE ARTESANÍA ---
    DURABILITY_BONUS("Bonus de Durabilidad", NamedTextColor.WHITE);

    private final String displayName;
    private final NamedTextColor color;

    AttributeType(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    /** Nombre legible del atributo. */
    public String getDisplayName() {
        return displayName;
    }

    /** Color Adventure asociado al atributo. */
    public NamedTextColor getColor() {
        return color;
    }

    /** Devuelve un Component formateado para Lore, Menús, etc. */
    public Component format(double value) {
        return Component.text(displayName + ": +" + value, color);
    }

    /**
     * Convierte un nombre (enum.name() o displayName) en AttributeType.
     * Ejemplos válidos: "STRENGTH", "Fuerza".
     */
    public static AttributeType fromName(String name) {
        for (AttributeType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null; // o lanza IllegalArgumentException si prefieres
    }
}
