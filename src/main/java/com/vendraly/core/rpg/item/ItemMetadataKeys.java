package com.vendraly.core.rpg.item;

import com.vendraly.core.Main;
import com.vendraly.rpg.AttributeType;
import org.bukkit.NamespacedKey;

import java.util.EnumMap;
import java.util.Map;

/**
 * Define todas las claves NamespacedKey utilizadas para el PDC de ítems RPG.
 * Centraliza los metadatos para evitar strings hardcodeados en otras clases.
 *
 * Ahora incluye un mapper entre AttributeType ↔ NamespacedKey para simplificar la escritura/lectura.
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
    public final NamespacedKey REQ_STAT_SPEED;
    public final NamespacedKey REQ_STAT_HEALTH;
    public final NamespacedKey REQ_STAT_AGILITY;

    // --- Requisitos de habilidades (skills) ---
    public final NamespacedKey REQ_SKILL_BLACKSMITHING;
    public final NamespacedKey REQ_SKILL_TAILORING;
    public final NamespacedKey REQ_SKILL_MINING;
    public final NamespacedKey REQ_SKILL_ALCHEMY;
    public final NamespacedKey REQ_SKILL_WOODCUTTING;

    // --- Bonificaciones de stats ---
    public final NamespacedKey BONUS_STAT_STRENGTH;
    public final NamespacedKey BONUS_STAT_DEFENSE;
    public final NamespacedKey BONUS_STAT_SPEED;
    public final NamespacedKey BONUS_STAT_HEALTH;
    public final NamespacedKey BONUS_STAT_STAMINA_MAX;
    public final NamespacedKey BONUS_STAT_AGILITY;

    // --- Bonificaciones avanzadas ---
    public final NamespacedKey BONUS_STAT_ATTACK_SPEED;
    public final NamespacedKey BONUS_STAT_ATTACK_RANGE;
    public final NamespacedKey BONUS_STAT_HEALTH_REGEN;
    public final NamespacedKey BONUS_STAT_CRITICAL_CHANCE;
    public final NamespacedKey BONUS_DURABILITY;

    // --- Mapper: AttributeType -> NamespacedKey ---
    private final Map<AttributeType, NamespacedKey> attributeKeyMap = new EnumMap<>(AttributeType.class);

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
        REQ_STAT_DEFENSE = new NamespacedKey(plugin, "req_defense");
        REQ_STAT_SPEED = new NamespacedKey(plugin, "req_speed");
        REQ_STAT_HEALTH = new NamespacedKey(plugin, "req_health");
        REQ_STAT_AGILITY = new NamespacedKey(plugin, "req_agility");

        // Skills
        REQ_SKILL_BLACKSMITHING = new NamespacedKey(plugin, "req_blacksmithing");
        REQ_SKILL_TAILORING = new NamespacedKey(plugin, "req_tailoring");
        REQ_SKILL_MINING = new NamespacedKey(plugin, "req_mining");
        REQ_SKILL_ALCHEMY = new NamespacedKey(plugin, "req_alchemy");
        REQ_SKILL_WOODCUTTING = new NamespacedKey(plugin, "req_woodcutting");

        // Bonificaciones
        BONUS_STAT_STRENGTH = new NamespacedKey(plugin, "bonus_strength");
        BONUS_STAT_DEFENSE = new NamespacedKey(plugin, "bonus_defense");
        BONUS_STAT_SPEED = new NamespacedKey(plugin, "bonus_speed");
        BONUS_STAT_HEALTH = new NamespacedKey(plugin, "bonus_health");
        BONUS_STAT_STAMINA_MAX = new NamespacedKey(plugin, "bonus_stamina");
        BONUS_STAT_AGILITY = new NamespacedKey(plugin, "bonus_agility");

        // Avanzados
        BONUS_STAT_ATTACK_SPEED = new NamespacedKey(plugin, "bonus_attack_speed");
        BONUS_STAT_ATTACK_RANGE = new NamespacedKey(plugin, "bonus_attack_range");
        BONUS_STAT_HEALTH_REGEN = new NamespacedKey(plugin, "bonus_health_regen");
        BONUS_STAT_CRITICAL_CHANCE = new NamespacedKey(plugin, "bonus_crit_chance");
        BONUS_DURABILITY = new NamespacedKey(plugin, "bonus_durability");

        // --- Cargar el mapper ---
        initAttributeMapper();
    }

    private void initAttributeMapper() {
        attributeKeyMap.put(AttributeType.STRENGTH, BONUS_STAT_STRENGTH);
        attributeKeyMap.put(AttributeType.DEFENSE, BONUS_STAT_DEFENSE);
        attributeKeyMap.put(AttributeType.SPEED, BONUS_STAT_SPEED);
        attributeKeyMap.put(AttributeType.HEALTH, BONUS_STAT_HEALTH);
        attributeKeyMap.put(AttributeType.STAMINA, BONUS_STAT_STAMINA_MAX);
        attributeKeyMap.put(AttributeType.AGILITY, BONUS_STAT_AGILITY);
        attributeKeyMap.put(AttributeType.ATTACK_SPEED, BONUS_STAT_ATTACK_SPEED);
        attributeKeyMap.put(AttributeType.ATTACK_RANGE, BONUS_STAT_ATTACK_RANGE);
        attributeKeyMap.put(AttributeType.HEALTH_REGEN, BONUS_STAT_HEALTH_REGEN);
        attributeKeyMap.put(AttributeType.CRITICAL_CHANCE, BONUS_STAT_CRITICAL_CHANCE);
        attributeKeyMap.put(AttributeType.DURABILITY_BONUS, BONUS_DURABILITY);
    }

    // --- Mapper inverso: NamespacedKey -> AttributeType ---
    private final Map<NamespacedKey, AttributeType> keyAttributeMap = new HashMap<>();

    private void initAttributeMapper() {
        attributeKeyMap.put(AttributeType.STRENGTH, BONUS_STAT_STRENGTH);
        attributeKeyMap.put(AttributeType.DEFENSE, BONUS_STAT_DEFENSE);
        attributeKeyMap.put(AttributeType.SPEED, BONUS_STAT_SPEED);
        attributeKeyMap.put(AttributeType.HEALTH, BONUS_STAT_HEALTH);
        attributeKeyMap.put(AttributeType.STAMINA, BONUS_STAT_STAMINA_MAX);
        attributeKeyMap.put(AttributeType.AGILITY, BONUS_STAT_AGILITY);
        attributeKeyMap.put(AttributeType.ATTACK_SPEED, BONUS_STAT_ATTACK_SPEED);
        attributeKeyMap.put(AttributeType.ATTACK_RANGE, BONUS_STAT_ATTACK_RANGE);
        attributeKeyMap.put(AttributeType.HEALTH_REGEN, BONUS_STAT_HEALTH_REGEN);
        attributeKeyMap.put(AttributeType.CRITICAL_CHANCE, BONUS_STAT_CRITICAL_CHANCE);
        attributeKeyMap.put(AttributeType.DURABILITY_BONUS, BONUS_DURABILITY);

        // rellenamos también el inverso
        attributeKeyMap.forEach((attr, key) -> keyAttributeMap.put(key, attr));
    }

    public NamespacedKey getKeyForAttribute(AttributeType type) {
        return attributeKeyMap.get(type);
    }

    public AttributeType getAttributeForKey(NamespacedKey key) {
        return keyAttributeMap.get(key);
    }

}
