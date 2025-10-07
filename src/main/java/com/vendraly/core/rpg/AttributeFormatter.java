package com.vendraly.core.rpg;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper global para convertir atributos RPG a lore visible en ítems.
 */
public class AttributeFormatter {

    /**
     * Convierte un Map de atributos a una lista de Component lista para lore.
     * Ejemplo de salida:
     * - "Fuerza: +10"
     * - "Defensa: +5"
     * - "Vida Máxima: +20"
     */
    public static List<Component> formatAttributes(Map<AttributeType, Double> attributes) {
        List<Component> lore = new ArrayList<>();

        for (Map.Entry<AttributeType, Double> entry : attributes.entrySet()) {
            AttributeType type = entry.getKey();
            double value = entry.getValue();

            // Usamos el format() de AttributeType directamente
            lore.add(type.format(value));
        }

        return lore;
    }
}
