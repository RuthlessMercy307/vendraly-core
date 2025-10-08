package com.vendraly.core.rpg.item;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.ability.AbilityType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemLoreUpdater {

    private final ItemMetadataKeys keys;
    private final Main plugin;
    private final NamespacedKey MENU_ITEM_KEY;
    private final NamespacedKey ITEM_LEVEL_KEY;

    public ItemLoreUpdater(Main plugin, ItemMetadataKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        this.MENU_ITEM_KEY = new NamespacedKey(plugin, "rpg_menu_item");
        this.ITEM_LEVEL_KEY = new NamespacedKey(plugin, "item_level");
    }

    /**
     * Actualiza lore y PDC de un ítem RPG.
     * Si el ítem es vanilla elegible, lo inicializa con valores base.
     */
    public void updateLoreAndPDC(ItemStack item, Player player, RPGStats stats) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(MENU_ITEM_KEY, PersistentDataType.BYTE)) return; // no tocar ítems de menú

        // Inicialización base si aún no es RPG
        if (!pdc.has(keys.ITEM_QUALITY, PersistentDataType.STRING)) {
            if (isRPGEligibleMaterial(item.getType())) {
                applyBasePDC(item);
                meta = item.getItemMeta();
                if (meta == null) return;
                pdc = meta.getPersistentDataContainer();
            } else {
                return;
            }
        }

        // Renderizar lore
        List<String> lore = renderLore(pdc, player, stats);
        meta.setLore(lore);

        // Aplicar atributos reales (limpia y reaplica)
        applyAttributeModifiers(item, meta, pdc);

        item.setItemMeta(meta);
    }

    // ---------------------------

    public boolean isRPGEligibleMaterial(Material material) {
        String name = material.name();
        return name.contains("_SWORD") || name.contains("_PICKAXE") || name.contains("_AXE") ||
                name.contains("_SHOVEL") || name.contains("_HOE") ||
                name.contains("_HELMET") || name.contains("_CHESTPLATE") ||
                name.contains("_LEGGINGS") || name.contains("_BOOTS");
    }

    private void applyBasePDC(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String quality = "COMUN";
        String abilityName = "";
        String name = item.getType().name();

        if (name.contains("_SWORD") || name.contains("_PICKAXE") || name.contains("_AXE") ||
                name.contains("_SHOVEL") || name.contains("_HOE")) {
            abilityName = AbilityType.BLACKSMITHING.name();
        } else if (name.contains("_HELMET") || name.contains("_CHESTPLATE") ||
                name.contains("_LEGGINGS") || name.contains("_BOOTS")) {
            abilityName = AbilityType.TAILORING.name();
        }

        // requisitos básicos (estos pueden quedarse en INTEGER)
        pdc.set(keys.REQ_STAT_STRENGTH, PersistentDataType.INTEGER, 0);
        pdc.set(keys.REQ_STAT_DEFENSE, PersistentDataType.INTEGER, 0);
        pdc.set(keys.REQ_STAT_STAMINA, PersistentDataType.INTEGER, 0);
        pdc.set(keys.REQ_STAT_HEALTH, PersistentDataType.INTEGER, 0);
        pdc.set(keys.REQ_STAT_AGILITY, PersistentDataType.INTEGER, 0);
        pdc.set(keys.REQ_SKILL_BLACKSMITHING, PersistentDataType.INTEGER, 0);
        pdc.set(keys.REQ_SKILL_TAILORING, PersistentDataType.INTEGER, 0);

        // calidad + habilidad + nivel
        pdc.set(keys.ITEM_QUALITY, PersistentDataType.STRING, quality);
        pdc.set(keys.ITEM_ABILITY_TYPE, PersistentDataType.STRING, abilityName);
        pdc.set(ITEM_LEVEL_KEY, PersistentDataType.INTEGER, 1);

        // inicializar bonus (todo DOUBLE para evitar el error de DoubleTag vs Integer)
        pdc.set(keys.BONUS_STAT_STRENGTH,       PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_DEFENSE,        PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_MOVEMENT_SPEED, PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_HEALTH,         PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_STAMINA_MAX,    PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_AGILITY,        PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_ATTACK_SPEED,   PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_ATTACK_RANGE,   PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_HEALTH_REGEN,   PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_STAT_CRITICAL_CHANCE,PersistentDataType.DOUBLE, 0.0);
        pdc.set(keys.BONUS_DURABILITY,          PersistentDataType.DOUBLE, 0.0);

        item.setItemMeta(meta); // guardar cambios
    }

    private List<String> renderLore(PersistentDataContainer pdc, Player player, RPGStats stats) {
        List<String> lore = new ArrayList<>();

        // calidad y nivel
        String quality = pdc.getOrDefault(keys.ITEM_QUALITY, PersistentDataType.STRING, "COMUN");
        int itemLevel = pdc.getOrDefault(ITEM_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        String abilityName = pdc.getOrDefault(keys.ITEM_ABILITY_TYPE, PersistentDataType.STRING, AbilityType.BLACKSMITHING.name());

        lore.add(ChatColor.GRAY + "Calidad: " + getQualityColor(quality) + quality);
        lore.add(ChatColor.GRAY + "Nivel del Ítem: " + ChatColor.YELLOW + itemLevel);
        lore.add("");

        // requisitos
        boolean hasReqs = false;
        StringBuilder reqs = new StringBuilder(ChatColor.YELLOW + "" + ChatColor.BOLD + "REQUISITOS:");
        int strReq = pdc.getOrDefault(keys.REQ_STAT_STRENGTH, PersistentDataType.INTEGER, 0);
        int defReq = pdc.getOrDefault(keys.REQ_STAT_DEFENSE, PersistentDataType.INTEGER, 0);
        int agiReq = pdc.getOrDefault(keys.REQ_STAT_AGILITY, PersistentDataType.INTEGER, 0);
        int skillReq = pdc.getOrDefault(keys.REQ_SKILL_BLACKSMITHING, PersistentDataType.INTEGER, 0)
                + pdc.getOrDefault(keys.REQ_SKILL_TAILORING, PersistentDataType.INTEGER, 0);

        if (strReq > 0)   { hasReqs = true; reqs.append("\n").append(" - Fuerza: ").append(strReq); }
        if (defReq > 0)   { hasReqs = true; reqs.append("\n").append(" - Defensa: ").append(defReq); }
        if (agiReq > 0)   { hasReqs = true; reqs.append("\n").append(" - Agilidad: ").append(agiReq); }
        if (skillReq > 0) { hasReqs = true; reqs.append("\n").append(" - Habilidad: ").append(skillReq); }

        if (hasReqs) {
            lore.add(reqs.toString());
            lore.add("");
        }

        // bonus
        double strBonus  = pdc.getOrDefault(keys.BONUS_STAT_STRENGTH,        PersistentDataType.DOUBLE, 0.0);
        double defBonus  = pdc.getOrDefault(keys.BONUS_STAT_DEFENSE,         PersistentDataType.DOUBLE, 0.0);
        double critBonus = pdc.getOrDefault(keys.BONUS_STAT_CRITICAL_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double regenBonus= pdc.getOrDefault(keys.BONUS_STAT_HEALTH_REGEN,    PersistentDataType.DOUBLE, 0.0);

        if (strBonus > 0 || defBonus > 0 || critBonus > 0 || regenBonus > 0) {
            lore.add(ChatColor.AQUA + "" + ChatColor.BOLD + "BONUS:");
            if (strBonus  > 0) lore.add(" + " + strBonus  + " Fuerza");
            if (defBonus  > 0) lore.add(" + " + defBonus  + " Defensa");
            if (critBonus > 0) lore.add(" + " + critBonus + "% Crítico");
            if (regenBonus> 0) lore.add(" + " + regenBonus+ " Regeneración Vida");
        }

        return lore;
    }

    private ChatColor getQualityColor(String quality) {
        switch (quality.toUpperCase()) {
            case "COMUN":      return ChatColor.WHITE;
            case "RARO":       return ChatColor.GREEN;
            case "EPICO":      return ChatColor.LIGHT_PURPLE;
            case "LEGENDARIO": return ChatColor.GOLD;
            case "MITICO":     return ChatColor.RED;
            default:           return ChatColor.GRAY;
        }
    }

    /** Aplica atributos vanilla al item (sólo visual/soporte; tu cálculo real es externo). */
    private void applyAttributeModifiers(ItemStack item, ItemMeta meta, PersistentDataContainer pdc) {
        // Limpia modifiers viejos: evitar problemas con Multimap
        meta.setAttributeModifiers(null);

        // Leemos los bonus como DOUBLE (ya unificados)
        double defBonus = pdc.getOrDefault(keys.BONUS_STAT_DEFENSE, PersistentDataType.DOUBLE, 0.0);
        double hpBonus  = pdc.getOrDefault(keys.BONUS_STAT_HEALTH,  PersistentDataType.DOUBLE, 0.0);
        double strBonus = pdc.getOrDefault(keys.BONUS_STAT_STRENGTH,PersistentDataType.DOUBLE, 0.0);

        // Elegimos el grupo de slot adecuado para que el atributo aplique donde corresponde
        EquipmentSlotGroup group = resolveSlotGroup(item.getType());

        // Usar constructores NO deprecados (NamespacedKey + Operation + EquipmentSlotGroup)
        if (defBonus > 0) {
            meta.addAttributeModifier(
                    Attribute.ARMOR,
                    new AttributeModifier(
                            new NamespacedKey(plugin, "rpg-armor"),
                            defBonus,
                            AttributeModifier.Operation.ADD_NUMBER,
                            group
                    )
            );
        }

        if (hpBonus > 0) {
            meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    new AttributeModifier(
                            new NamespacedKey(plugin, "rpg-health"),
                            hpBonus,
                            AttributeModifier.Operation.ADD_NUMBER,
                            group
                    )
            );
        }

        if (strBonus > 0) {
            meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(
                            new NamespacedKey(plugin, "rpg-strength"),
                            strBonus,
                            AttributeModifier.Operation.ADD_NUMBER,
                            group
                    )
            );
        }
    }

    /** Determina el grupo de slot en el que debe aplicar el atributo. */
    private EquipmentSlotGroup resolveSlotGroup(Material type) {
        String name = type.name();

        // Armas / herramientas → mano
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE") ||
                name.endsWith("_AXE")   || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE")) {
            return EquipmentSlotGroup.HAND;
        }

        // Piezas de armadura → slot específico
        if (name.endsWith("_HELMET"))     return EquipmentSlotGroup.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (name.endsWith("_LEGGINGS"))   return EquipmentSlotGroup.LEGS;
        if (name.endsWith("_BOOTS"))      return EquipmentSlotGroup.FEET;

        // Por defecto (por si agregas más cosas en el futuro)
        return EquipmentSlotGroup.ANY;
    }
}
