package com.vendraly.core.rpg.item;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.AttributeType;
import org.bukkit.NamespacedKey;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Define todas las claves NamespacedKey utilizadas para el PDC de ítems RPG.
 * Centraliza los metadatos para evitar strings hardcodeados en otras clases.
 * Incluye mapper AttributeType ↔ NamespacedKey.
 */
public class ItemMetadataKeys {

    private final Main plugin;

    // --- Atributos globales del ítem ---
    public final NamespacedKey ITEM_QUALITY;
    public final NamespacedKey ITEM_ABILITY_TYPE;
    public final NamespacedKey ATTRIBUTE_COUNT;
    public final NamespacedKey STAT_LIMIT_MAX_DMG;
    public final NamespacedKey INITIAL_DAMAGE_PERCENT;

    // --- Requisitos de estadísticas del jugador ---
    public final NamespacedKey REQ_STAT_STRENGTH;
    public final NamespacedKey REQ_STAT_DEFENSE;
    public final NamespacedKey REQ_STAT_AGILITY;
    public final NamespacedKey REQ_STAT_HEALTH;
    public final NamespacedKey REQ_STAT_STAMINA;
    public final NamespacedKey REQ_STAT_SPEED;

    // --- Requisitos de habilidades (skills) ---
    public final NamespacedKey REQ_SKILL_BLACKSMITHING;
    public final NamespacedKey REQ_SKILL_TAILORING;
    public final NamespacedKey REQ_SKILL_MINING;
    public final NamespacedKey REQ_SKILL_ALCHEMY;
    public final NamespacedKey REQ_SKILL_WOODCUTTING;

    // --- Bonificaciones de stats ---
    public final NamespacedKey BONUS_STAT_STRENGTH;
    public final NamespacedKey BONUS_STAT_DEFENSE;
    public final NamespacedKey BONUS_STAT_AGILITY;
    public final NamespacedKey BONUS_STAT_HEALTH;
    public final NamespacedKey BONUS_STAT_STAMINA_MAX;
    public final NamespacedKey BONUS_STAT_MOVEMENT_SPEED;
    public final NamespacedKey BONUS_STAT_MINING;
    public final NamespacedKey BONUS_STAT_WOODCUTTING;
    public final NamespacedKey BONUS_STAT_HEALTH_REGEN;

    // --- Bonificaciones avanzadas ---
    public final NamespacedKey BONUS_STAT_ATTACK_SPEED;
    public final NamespacedKey BONUS_STAT_ATTACK_RANGE;
    public final NamespacedKey BONUS_STAT_CRITICAL_CHANCE;
    public final NamespacedKey BONUS_DURABILITY;

    // --- Bonos de skills por equipo ---
    public final NamespacedKey BONUS_SKILL_BLACKSMITH;
    public final NamespacedKey BONUS_SKILL_TAILOR;
    public final NamespacedKey BONUS_SKILL_APOTHECARY;

    // --- Mapper: AttributeType -> NamespacedKey ---
    private final Map<AttributeType, NamespacedKey> attributeKeyMap = new EnumMap<>(AttributeType.class);
    private final Map<NamespacedKey, AttributeType> keyAttributeMap = new HashMap<>();

    public ItemMetadataKeys(Main plugin) {
        this.plugin = plugin;

        // --- Inicialización ---
        ITEM_QUALITY = new NamespacedKey(plugin, "item_quality");
        ITEM_ABILITY_TYPE = new NamespacedKey(plugin, "item_ability");
        ATTRIBUTE_COUNT = new NamespacedKey(plugin, "attribute_count");
        STAT_LIMIT_MAX_DMG = new NamespacedKey(plugin, "stat_limit_max_dmg");
        INITIAL_DAMAGE_PERCENT = new NamespacedKey(plugin, "initial_damage_percent");

        // Requisitos base
        REQ_STAT_STRENGTH = new NamespacedKey(plugin, "req_strength");
        REQ_STAT_DEFENSE  = new NamespacedKey(plugin, "req_defense");
        REQ_STAT_AGILITY  = new NamespacedKey(plugin, "req_agility");
        REQ_STAT_HEALTH   = new NamespacedKey(plugin, "req_health");
        REQ_STAT_STAMINA  = new NamespacedKey(plugin, "req_stamina");
        REQ_STAT_SPEED  = new NamespacedKey(plugin, "req_speed");


        // Skills
        REQ_SKILL_BLACKSMITHING = new NamespacedKey(plugin, "req_blacksmithing");
        REQ_SKILL_TAILORING     = new NamespacedKey(plugin, "req_tailoring");
        REQ_SKILL_MINING        = new NamespacedKey(plugin, "req_mining");
        REQ_SKILL_ALCHEMY       = new NamespacedKey(plugin, "req_alchemy");
        REQ_SKILL_WOODCUTTING   = new NamespacedKey(plugin, "req_woodcutting");

        // Bonos stats
        BONUS_STAT_STRENGTH        = new NamespacedKey(plugin, "bonus_strength");
        BONUS_STAT_DEFENSE         = new NamespacedKey(plugin, "bonus_defense");
        BONUS_STAT_AGILITY         = new NamespacedKey(plugin, "bonus_agility");
        BONUS_STAT_HEALTH          = new NamespacedKey(plugin, "bonus_health");
        BONUS_STAT_STAMINA_MAX     = new NamespacedKey(plugin, "bonus_stamina_max");
        BONUS_STAT_MOVEMENT_SPEED  = new NamespacedKey(plugin, "bonus_movement_speed");
        BONUS_STAT_MINING          = new NamespacedKey(plugin, "bonus_mining");
        BONUS_STAT_WOODCUTTING     = new NamespacedKey(plugin, "bonus_woodcutting");
        BONUS_STAT_HEALTH_REGEN    = new NamespacedKey(plugin, "bonus_health_regen");

        // Avanzados
        BONUS_STAT_ATTACK_SPEED    = new NamespacedKey(plugin, "bonus_attack_speed");
        BONUS_STAT_ATTACK_RANGE    = new NamespacedKey(plugin, "bonus_attack_range");
        BONUS_STAT_CRITICAL_CHANCE = new NamespacedKey(plugin, "bonus_crit_chance");
        BONUS_DURABILITY           = new NamespacedKey(plugin, "bonus_durability");

        // Skills
        BONUS_SKILL_BLACKSMITH = new NamespacedKey(plugin, "bonus_skill_blacksmith");
        BONUS_SKILL_TAILOR     = new NamespacedKey(plugin, "bonus_skill_tailor");
        BONUS_SKILL_APOTHECARY = new NamespacedKey(plugin, "bonus_skill_apothecary");

        // Cargar mapper
        initAttributeMapper();
    }

    private void initAttributeMapper() {
        attributeKeyMap.put(AttributeType.STRENGTH,        BONUS_STAT_STRENGTH);
        attributeKeyMap.put(AttributeType.DEFENSE,         BONUS_STAT_DEFENSE);
        attributeKeyMap.put(AttributeType.AGILITY,         BONUS_STAT_AGILITY);
        attributeKeyMap.put(AttributeType.HEALTH,          BONUS_STAT_HEALTH);
        attributeKeyMap.put(AttributeType.STAMINA_MAX,     BONUS_STAT_STAMINA_MAX);
        attributeKeyMap.put(AttributeType.MOVEMENT_SPEED,  BONUS_STAT_MOVEMENT_SPEED);
        attributeKeyMap.put(AttributeType.MINING_SPEED,    BONUS_STAT_MINING);
        attributeKeyMap.put(AttributeType.WOODCUTTING_SPEED, BONUS_STAT_WOODCUTTING);
        attributeKeyMap.put(AttributeType.HEALTH_REGEN,    BONUS_STAT_HEALTH_REGEN);
        attributeKeyMap.put(AttributeType.ATTACK_SPEED,    BONUS_STAT_ATTACK_SPEED);
        attributeKeyMap.put(AttributeType.ATTACK_RANGE,    BONUS_STAT_ATTACK_RANGE);
        attributeKeyMap.put(AttributeType.CRITICAL_CHANCE, BONUS_STAT_CRITICAL_CHANCE);
        attributeKeyMap.put(AttributeType.DURABILITY_BONUS,BONUS_DURABILITY);

        keyAttributeMap.clear();
        attributeKeyMap.forEach((attr, key) -> keyAttributeMap.put(key, attr));
    }

    public NamespacedKey getKeyForAttribute(AttributeType type) {
        return attributeKeyMap.get(type);
    }

    public AttributeType getAttributeForKey(NamespacedKey key) {
        return keyAttributeMap.get(key);
    }
}
