package com.vendraly.core.rpg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Define los tipos de atributos RPG que pueden tener las armas y armaduras.
 * Utiliza un nombre legible (displayName) y un color asociado
 * para la presentación en el juego (Lore, Menús).
 */
public enum AttributeType {
    // ATRIBUTOS DE COMBATE Y ESTADÍSTICAS
    STRENGTH("Fuerza", NamedTextColor.RED),
    AGILITY("Agilidad", NamedTextColor.YELLOW),
    DEFENSE("Defensa", NamedTextColor.BLUE),          // Defensa física (reducción de daño plano)
    ATTACK_SPEED("Velocidad de Ataque", NamedTextColor.AQUA),
    ATTACK_RANGE("Alcance", NamedTextColor.LIGHT_PURPLE),

    // ATRIBUTOS DE RESISTENCIA Y VIDA
    HEALTH("Vida Máxima", NamedTextColor.DARK_RED),
    STAMINA_MAX("Estamina Máxima", NamedTextColor.GREEN),
    HEALTH_REGEN("Regeneración de Vida", NamedTextColor.DARK_GREEN),

    // ATRIBUTOS DE HERRERÍA
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
        return null; // o lanza IllegalArgumentException si quieres ser estricto
    }
}
