package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.ability.AbilityType;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import com.vendraly.core.rpg.AttributeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Generador de ítems RPG:
 * Persiste nivel, calidad, atributos, requisitos y lore usando PDC.
 * La lógica de cálculo ahora la hace CraftingManager.
 */
public class RPGItemGenerator {

    private final Main plugin;
    private final ItemMetadataKeys keys;
    private final NamespacedKey ITEM_LEVEL_KEY;

    public RPGItemGenerator(Main plugin, ItemMetadataKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        this.ITEM_LEVEL_KEY = new NamespacedKey(plugin, "item_level");
    }

    /* ---------------------------
       Personalización visual + PDC
       --------------------------- */
    public ItemStack customizeItem(ItemStack item, String quality, int level, AbilityType ability,
                                   Map<AttributeType, Double> attributes, Map<String, Integer> extraPDCData) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Guardar en PDC
        pdc.set(keys.ITEM_QUALITY, PersistentDataType.STRING, quality);
        pdc.set(keys.ITEM_ABILITY_TYPE, PersistentDataType.STRING, ability.name());
        pdc.set(ITEM_LEVEL_KEY, PersistentDataType.INTEGER, level);
        pdc.set(keys.ATTRIBUTE_COUNT, PersistentDataType.INTEGER, attributes.size());

        // Guardar atributos usando el mapper
        attributes.forEach((type, value) -> {
            NamespacedKey key = keys.getKeyForAttribute(type);
            if (key != null) {
                pdc.set(key, PersistentDataType.DOUBLE, value);
            }
        });

        // Guardar PDC extra (ej. requisitos)
        if (extraPDCData != null) {
            extraPDCData.forEach((k, v) -> {
                NamespacedKey extraKey = new NamespacedKey(plugin, k.toLowerCase());
                pdc.set(extraKey, PersistentDataType.INTEGER, v);
            });
        }

        // Lore RPG
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Nivel: " + level).color(NamedTextColor.GRAY));
        lore.add(Component.text("Habilidad: " + ability.name()).color(NamedTextColor.AQUA));
        lore.add(Component.text(" "));

        // Atributos
        lore.add(Component.text("Atributos:").color(NamedTextColor.YELLOW));
        attributes.forEach((type, value) ->
                lore.add(Component.text("+ " + value + " " + type.name())
                        .color(NamedTextColor.GREEN))
        );
        lore.add(Component.text(" "));

        // Requisitos (si los hay)
        if (extraPDCData != null && !extraPDCData.isEmpty()) {
            lore.add(Component.text("Requisitos:").color(NamedTextColor.RED));
            extraPDCData.forEach((k, v) -> {
                String clean = k.replace("req_", "").toUpperCase();
                lore.add(Component.text("- " + clean + ": " + v).color(NamedTextColor.DARK_RED));
            });
            lore.add(Component.text(" "));
        }

        // Calidad
        lore.add(QualityUtility.getLoreLineForQuality(quality));
        meta.lore(lore);

        // Nombre final
        Component finalName = Component.text("[")
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(quality)
                        .color(QualityUtility.getNamedColorForQuality(quality))
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("] ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(item.getType().name().replace('_', ' '))
                        .color(NamedTextColor.WHITE));

        meta.displayName(finalName);

        item.setItemMeta(meta);
        return item;
    }

    /* ---------------------------
       Lectura de atributos y requisitos desde PDC
       --------------------------- */

    public Map<AttributeType, Double> readAttributesFromPDC(ItemStack item) {
        Map<AttributeType, Double> attributes = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return attributes;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return attributes;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (NamespacedKey key : pdc.getKeys()) {
            AttributeType type = keys.getAttributeForKey(key);
            if (type != null) {
                Double value = pdc.get(key, PersistentDataType.DOUBLE);
                if (value != null) {
                    attributes.put(type, value);
                }
            }
        }

        return attributes;
    }

    public Map<String, Integer> readRequirementsFromPDC(ItemStack item) {
        Map<String, Integer> reqs = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return reqs;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return reqs;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        readReq(pdc, keys.REQ_STAT_STRENGTH, "strength", reqs);
        readReq(pdc, keys.REQ_STAT_DEFENSE, "defense", reqs);
        readReq(pdc, keys.REQ_STAT_SPEED, "speed", reqs);
        readReq(pdc, keys.REQ_STAT_HEALTH, "health", reqs);
        readReq(pdc, keys.REQ_STAT_AGILITY, "agility", reqs);

        return reqs;
    }

    private void readReq(PersistentDataContainer pdc, NamespacedKey key, String label, Map<String, Integer> reqs) {
        Integer val = pdc.get(key, PersistentDataType.INTEGER);
        if (val != null && val > 0) {
            reqs.put(label, val);
        }
    }

    /* ---------------------------
   Generación principal de ítems RPG
   --------------------------- */
    public ItemStack generateRPGItem(ItemStack baseItem,
                                     int playerSkillLevel,
                                     AbilityType ability,
                                     int difficultyLevel,
                                     Map<String, Integer> basePDCData) {
        // 1. Determinar calidad del ítem
        String quality;
        if (playerSkillLevel >= difficultyLevel + 10) {
            quality = "Legendario";
        } else if (playerSkillLevel >= difficultyLevel) {
            quality = "Raro";
        } else if (playerSkillLevel >= difficultyLevel * 0.7) {
            quality = "Común";
        } else {
            quality = "Defectuoso";
        }

        // 2. Nivel final del ítem (capado por dificultad)
        int finalLevel = Math.min(playerSkillLevel, difficultyLevel);

        // 3. Atributos básicos del ítem
        Map<AttributeType, Double> attributes = new HashMap<>();
        attributes.put(AttributeType.STRENGTH, (double) (1 + finalLevel / 5));
        attributes.put(AttributeType.DEFENSE, (double) (1 + finalLevel / 6));

        // 4. Crear ítem customizado con PDC + lore
        return customizeItem(baseItem, quality, finalLevel, ability, attributes, basePDCData);
    }

}
