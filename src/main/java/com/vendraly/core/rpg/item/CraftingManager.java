package com.vendraly.core.rpg.item;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGItemGenerator;
import com.vendraly.core.rpg.ability.AbilityType;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import com.vendraly.core.rpg.AttributeType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * CraftingManager
 *
 * Clase utilitaria (pura, sin Bukkit) que centraliza TODA la lógica probabilística del crafteo RPG:
 * - Nivel del ítem.
 * - Calidad del ítem.
 * - Durabilidad inicial.
 * - Atributos dependientes de categoría, calidad y nivel.
 * - Requisitos razonables.
 *
 * NOTA: Ahora se integra con RPGItemGenerator para persistir datos en PDC y lore.
 */
public final class CraftingManager {

    private CraftingManager() {}

    /* =====================================================
     * ====================  CONSTANTES  ====================
     * ===================================================== */

    private static final double LEVEL_MODE_RATIO = 0.80;
    private static final double LEVEL_MIN_STD = 2.0;
    private static final double LEVEL_STD_FACTOR = 0.20;

    private static final double QUALITY_SKILL_BONUS_PER_DELTA = 0.015;

    private static final int DURABILITY_COMMON_MIN  = 55;
    private static final int DURABILITY_COMMON_MAX  = 85;
    private static final int DURABILITY_RARE_MIN    = 75;
    private static final int DURABILITY_RARE_MAX    = 95;
    private static final int DURABILITY_EPIC_MIN    = 90;
    private static final int DURABILITY_EPIC_MAX    = 100;
    private static final int DURABILITY_LEG_MIN     = 95;
    private static final int DURABILITY_LEG_MAX     = 100;
    private static final int DURABILITY_MYTH_MIN    = 97;
    private static final int DURABILITY_MYTH_MAX    = 100;

    private static final Map<Quality, Integer> ATTRIBUTES_PER_QUALITY = Map.of(
            Quality.COMMON, 1,
            Quality.RARE, 2,
            Quality.EPIC, 3,
            Quality.LEGENDARY, 4,
            Quality.MYTHIC, 5
    );

    private static final double ATTR_BASE_PER_LEVEL = 0.25;
    private static final Map<Quality, Double> ATTR_QUALITY_MULTIPLIER = Map.of(
            Quality.COMMON, 1.00,
            Quality.RARE, 1.15,
            Quality.EPIC, 1.35,
            Quality.LEGENDARY, 1.60,
            Quality.MYTHIC, 1.85
    );

    private static final Map<MaterialTier, ReqTemplate> BASE_REQ_BY_TIER = Map.of(
            MaterialTier.WOOD,     new ReqTemplate(1,  1),
            MaterialTier.LEATHER,  new ReqTemplate(2,  2),
            MaterialTier.STONE,    new ReqTemplate(4,  3),
            MaterialTier.IRON,     new ReqTemplate(7,  5),
            MaterialTier.GOLD,     new ReqTemplate(8,  7),
            MaterialTier.DIAMOND,  new ReqTemplate(12, 10),
            MaterialTier.NETHERITE,new ReqTemplate(18, 14)
    );

    private static final Map<MaterialTier, double[]> QUALITY_WEIGHTS_BY_TIER = Map.of(
            MaterialTier.WOOD,      new double[]{0.68, 0.22, 0.08, 0.02, 0.00},
            MaterialTier.LEATHER,   new double[]{0.62, 0.24, 0.10, 0.03, 0.01},
            MaterialTier.STONE,     new double[]{0.58, 0.26, 0.11, 0.04, 0.01},
            MaterialTier.IRON,      new double[]{0.50, 0.28, 0.15, 0.06, 0.01},
            MaterialTier.GOLD,      new double[]{0.46, 0.30, 0.16, 0.07, 0.01},
            MaterialTier.DIAMOND,   new double[]{0.40, 0.32, 0.18, 0.08, 0.02},
            MaterialTier.NETHERITE, new double[]{0.34, 0.32, 0.20, 0.10, 0.04}
    );

    private static final Random RNG = new Random();

    /* =====================================================
     * =====================  PÚBLICO  ======================
     * ===================================================== */

    public static ItemRoll generateItemRoll(ItemContext ctx) {
        int itemLevel = generateItemLevel(ctx.playerSkillLevel());
        Quality quality = generateItemQuality(ctx.material(), ctx.playerSkillLevel());
        int attrCount = ATTRIBUTES_PER_QUALITY.getOrDefault(quality, 1);
        int durabilityPct = rollDurabilityPercent(quality);

        Map<AttributeType, Double> attributes = rollAttributes(ctx.category(), attrCount, itemLevel, quality);
        Requirements reqs = rollRequirements(ctx.material(), ctx.category());

        return new ItemRoll(itemLevel, quality, durabilityPct, attributes, reqs);
    }

    /** Construye directamente un ItemStack listo con lore y PDC. */
    public static ItemStack buildCraftedItem(Main plugin,
                                             ItemMetadataKeys keys,
                                             ItemStack baseItem,
                                             AbilityType ability,
                                             ItemContext ctx) {
        ItemRoll roll = generateItemRoll(ctx);
        RPGItemGenerator gen = new RPGItemGenerator(plugin, keys);

        Map<String,Integer> reqsPDC = new HashMap<>();
        reqsPDC.put("req_strength", roll.requirements().strength());
        reqsPDC.put("req_speed", roll.requirements().speed());

        return gen.customizeItem(
                baseItem,
                roll.quality().name(),
                roll.itemLevel(),
                ability,
                roll.attributes(),
                reqsPDC
        );
    }

    /* =====================================================
     * =====================  LÓGICA  =======================
     * ===================================================== */

    public static int generateItemLevel(int playerSkillLevel) {
        if (playerSkillLevel <= 1) return 1;

        double mode = Math.max(1.0, playerSkillLevel * LEVEL_MODE_RATIO);
        double std  = Math.max(LEVEL_MIN_STD, playerSkillLevel * LEVEL_STD_FACTOR);

        int attempts = 0;
        int level;
        do {
            double sample = RNG.nextGaussian() * std + mode;
            level = (int) Math.round(sample);
            attempts++;
            if (attempts > 8) break;
        } while (level < 1 || level > playerSkillLevel);

        level = Math.max(1, Math.min(level, playerSkillLevel));

        if (RNG.nextDouble() < 0.02) level = 1;
        if (RNG.nextDouble() < 0.02) level = playerSkillLevel;

        return level;
    }

    public static Quality generateItemQuality(Material material, int playerSkillLevel) {
        MaterialTier tier = guessTier(material);

        double[] base = QUALITY_WEIGHTS_BY_TIER.getOrDefault(tier, QUALITY_WEIGHTS_BY_TIER.get(MaterialTier.WOOD)).clone();

        int expected = BASE_REQ_BY_TIER.getOrDefault(tier, new ReqTemplate(1,1)).strength;
        int delta = Math.max(0, playerSkillLevel - expected);
        double bonus = (delta / 10.0) * QUALITY_SKILL_BONUS_PER_DELTA;

        double take = Math.min(bonus, base[0] * 0.5);
        base[0] -= take;
        base[1] += take * 0.55;
        base[2] += take * 0.30;
        base[3] += take * 0.12;
        base[4] += take * 0.03;

        normalize(base);

        int idx = weightedIndex(base);
        return Quality.fromIndex(idx);
    }

    public static int rollDurabilityPercent(Quality q) {
        return switch (q) {
            case COMMON -> uniform(DURABILITY_COMMON_MIN, DURABILITY_COMMON_MAX);
            case RARE -> uniform(DURABILITY_RARE_MIN, DURABILITY_RARE_MAX);
            case EPIC -> uniform(DURABILITY_EPIC_MIN, DURABILITY_EPIC_MAX);
            case LEGENDARY -> uniform(DURABILITY_LEG_MIN, DURABILITY_LEG_MAX);
            case MYTHIC -> uniform(DURABILITY_MYTH_MIN, DURABILITY_MYTH_MAX);
        };
    }

    public static Map<AttributeType, Double> rollAttributes(ItemCategory category, int count, int itemLevel, Quality quality) {
        Map<AttributeType, Double> out = new LinkedHashMap<>();

        List<AttributeType> pool = switch (category) {
            case WEAPON -> new ArrayList<>(List.of(
                    AttributeType.STRENGTH,
                    AttributeType.ATTACK_SPEED,
                    AttributeType.AGILITY
            ));
            case ARMOR -> new ArrayList<>(List.of(
                    AttributeType.DEFENSE,
                    AttributeType.HEALTH_REGEN,
                    AttributeType.AGILITY
            ));
            case TOOL -> new ArrayList<>(List.of(
                    AttributeType.ATTACK_SPEED,
                    AttributeType.AGILITY,
                    AttributeType.DURABILITY_BONUS
            ));
        };

        Collections.shuffle(pool, RNG);

        double qualMul = ATTR_QUALITY_MULTIPLIER.getOrDefault(quality, 1.0);
        int picks = Math.min(count, pool.size());

        for (int i = 0; i < picks; i++) {
            AttributeType type = pool.get(i);
            double base = ATTR_BASE_PER_LEVEL * itemLevel * qualMul;
            double value;

            switch (type) {
                case STRENGTH -> {
                    value = clamp(base * 1.4, 1.0, 9999.0);
                }
                case DEFENSE -> {
                    value = clamp(base * 0.8, 0.5, 50.0);
                }
                case ATTACK_SPEED -> {
                    value = clamp(base * 0.6, 0.1, 10.0);
                }
                case HEALTH_REGEN -> {
                    value = clamp(base * 0.12, 0.05, 5.0);
                }
                case AGILITY -> {
                    value = Math.min(25.0, 2.0 + base * 0.25);
                }
                case DURABILITY_BONUS -> {
                    value = Math.min(20.0, 3.0 + base * 0.15);
                }
                default -> value = base;
            }

            double jitter = 0.9 + (RNG.nextDouble() * 0.2);
            value *= jitter;

            out.put(type, round1(value));
        }

        return out;
    }

    public static Requirements rollRequirements(Material material, ItemCategory category) {
        MaterialTier tier = guessTier(material);
        ReqTemplate base = BASE_REQ_BY_TIER.getOrDefault(tier, new ReqTemplate(1,1));

        int strength = base.strength;
        int speed    = base.speed;

        switch (category) {
            case WEAPON -> {
                strength += uniform(0, 3);
                speed    += uniform(0, 2);
            }
            case ARMOR -> {
                strength += uniform(0, 2);
                speed    += uniform(0, 1);
            }
            case TOOL -> {
                strength += uniform(0, 1);
                speed    += uniform(1, 4);
            }
        }

        return new Requirements(Math.max(0, strength), Math.max(0, speed));
    }

    /* =====================================================
     * =====================  HELPERS  ======================
     * ===================================================== */

    private static MaterialTier guessTier(Material m) {
        if (m == null) return MaterialTier.WOOD;
        String name = m.name();
        if (name.contains("LEATHER")) return MaterialTier.LEATHER;
        if (name.contains("CHAINMAIL")) return MaterialTier.IRON;
        if (name.contains("IRON")) return MaterialTier.IRON;
        if (name.contains("GOLD")) return MaterialTier.GOLD;
        if (name.contains("DIAMOND")) return MaterialTier.DIAMOND;
        if (name.contains("NETHERITE")) return MaterialTier.NETHERITE;
        if (name.contains("WOOD")) return MaterialTier.WOOD;
        if (name.contains("STONE")) return MaterialTier.STONE;
        return MaterialTier.WOOD;
    }

    private static void normalize(double[] arr) {
        double sum = Arrays.stream(arr).sum();
        if (sum <= 0) return;
        for (int i = 0; i < arr.length; i++) arr[i] /= sum;
    }

    private static int weightedIndex(double[] weights) {
        double r = RNG.nextDouble(), acc = 0.0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r <= acc) return i;
        }
        return weights.length - 1;
    }

    private static int uniform(int min, int max) {
        if (max < min) return min;
        if (min == max) return min;
        return min + RNG.nextInt(max - min + 1);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /* =====================================================
     * =====================  TIPOS  ========================
     * ===================================================== */

    public enum ItemCategory { WEAPON, ARMOR, TOOL }
    public enum Quality { COMMON, RARE, EPIC, LEGENDARY, MYTHIC;
        public static Quality fromIndex(int idx) {
            return switch (idx) {
                case 0 -> COMMON;
                case 1 -> RARE;
                case 2 -> EPIC;
                case 3 -> LEGENDARY;
                default -> MYTHIC;
            };
        }
    }
    public enum MaterialTier { WOOD, LEATHER, STONE, IRON, GOLD, DIAMOND, NETHERITE }

    public record ItemContext(Material material, ItemCategory category, int playerSkillLevel) {}
    public record ItemRoll(int itemLevel, Quality quality, int durabilityPercent, Map<AttributeType, Double> attributes, Requirements requirements) {}
    public record Requirements(int strength, int speed) {}

    private static final class ReqTemplate {
        final int strength; final int speed;
        ReqTemplate(int strength, int speed) { this.strength = strength; this.speed = speed; }
    }
}
