package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class AttributeApplier {

    private final Main plugin;
    private final StatManager statManager;
    private final ItemMetadataKeys keys; // Puede ser nulo si Main.java falla al inicializarlo

    // Constantes de balance
    public static final double FLAT_DAMAGE_REDUCTION_PER_DEFENSE_POINT = 0.1;
    private static final double TAILORING_MAGIC_DEFENSE_PER_LEVEL = 1.0 / 5.0;
    private static final double BASE_MC_SPEED = 0.1;
    private static final double SPEED_IMPROVEMENT_PER_POINT = 0.0005;
    private static final double VANILLA_MAX_HEALTH = 20.0;

    // Atributos Bukkit
    private static final Attribute HEALTH = Attribute.MAX_HEALTH;
    private static final Attribute DAMAGE = Attribute.ATTACK_DAMAGE;
    private static final Attribute SPEED = Attribute.MOVEMENT_SPEED;

    private final NamespacedKey healthKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey speedKey;

    public AttributeApplier(Main plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.keys = plugin.getItemMetadataKeys();
        this.healthKey = new NamespacedKey(plugin, "rpg_health_mod");
        this.damageKey = new NamespacedKey(plugin, "rpg_damage_mod");
        this.speedKey = new NamespacedKey(plugin, "rpg_speed_mod");
    }

    public double getAbilityBonusMagicDefense(UUID uuid) {
        RPGStats stats = statManager.getStats(uuid);
        if (stats == null) return 0.0;
        // CORRECCIÓN: Usar getAbilityLevel con AbilityType para evitar el error de String/Enum
        int tailoringLevel = stats.getSkillLevel(StatManager.TAILORING);
        return tailoringLevel * TAILORING_MAGIC_DEFENSE_PER_LEVEL;
    }

    public double getRpgMaxHealth(RPGStats stats) {
        return stats.getMaxHealth();
    }

    // **************** MÉTODO CRÍTICO: RECALCULAR BONUS DE EQUIPO (CORREGIDO) ****************
    public void recalculateEquippedBonuses(Player player) {
        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        if (this.keys == null) {
            plugin.getLogger().severe("ItemMetadataKeys es NULL. Revisar inicialización en Main.java.");
            stats.resetEquipmentBonuses();
            return;
        }

        stats.resetEquipmentBonuses();

        int totalStrBonus = 0;
        int totalDefBonus = 0;
        int totalSpeedBonus = 0;
        int totalHealthBonus = 0;
        int totalStaminaBonus = 0;
        int totalMiningBonus = 0;
        int totalWoodcuttingBonus = 0;
        int totalRegenBonus = 0;
        int totalBlacksmithBonus = 0;
        int totalTailoringBonus = 0;
        int totalApothecaryBonus = 0;

        Collection<ItemStack> equippedItems = new ArrayList<>();
        equippedItems.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        equippedItems.add(player.getInventory().getItemInMainHand());
        equippedItems.add(player.getInventory().getItemInOffHand());

        for (ItemStack item : equippedItems) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(keys.ITEM_QUALITY, PersistentDataType.STRING)) continue; // Solo ítems RPG

            totalStrBonus += pdc.getOrDefault(keys.BONUS_STAT_STRENGTH, PersistentDataType.DOUBLE, 0.0).intValue();
            totalDefBonus += pdc.getOrDefault(keys.BONUS_STAT_DEFENSE, PersistentDataType.DOUBLE, 0.0).intValue();
            totalSpeedBonus += pdc.getOrDefault(keys.BONUS_STAT_MOVEMENT_SPEED, PersistentDataType.DOUBLE, 0.0).intValue();
            totalHealthBonus += pdc.getOrDefault(keys.BONUS_STAT_HEALTH, PersistentDataType.DOUBLE, 0.0).intValue();
            totalStaminaBonus += pdc.getOrDefault(keys.BONUS_STAT_STAMINA_MAX, PersistentDataType.DOUBLE, 0.0).intValue();

            totalMiningBonus += pdc.getOrDefault(keys.BONUS_STAT_MINING, PersistentDataType.DOUBLE, 0.0).intValue();
            totalWoodcuttingBonus += pdc.getOrDefault(keys.BONUS_STAT_WOODCUTTING, PersistentDataType.DOUBLE, 0.0).intValue();
            totalRegenBonus += pdc.getOrDefault(keys.BONUS_STAT_HEALTH_REGEN, PersistentDataType.DOUBLE, 0.0).intValue();

            totalBlacksmithBonus += pdc.getOrDefault(keys.BONUS_SKILL_BLACKSMITH, PersistentDataType.DOUBLE, 0.0).intValue();
            totalTailoringBonus  += pdc.getOrDefault(keys.BONUS_SKILL_TAILOR, PersistentDataType.DOUBLE, 0.0).intValue();
            totalApothecaryBonus += pdc.getOrDefault(keys.BONUS_SKILL_APOTHECARY, PersistentDataType.DOUBLE, 0.0).intValue();
        }

        stats.addBonusStrength(totalStrBonus);
        stats.addBonusDefense(totalDefBonus);
        stats.addBonusMovementSpeed(totalSpeedBonus);
        stats.addBonusHealth(totalHealthBonus);
        stats.addBonusStamina(totalStaminaBonus);

        stats.addBonusMiningSpeed(totalMiningBonus);
        stats.addBonusWoodcuttingSpeed(totalWoodcuttingBonus);
        stats.addBonusHealthRegen(totalRegenBonus);

        stats.addBonusSkill(StatManager.BLACKSMITHING, totalBlacksmithBonus);
        stats.addBonusSkill(StatManager.TAILORING, totalTailoringBonus);
        stats.addBonusSkill(StatManager.APOTHECARY, totalApothecaryBonus);
    }
    // **************** FIN DEL MÉTODO CRÍTICO ****************

    /**
     * Aplica todos los atributos RPG al jugador basándose en su objeto RPGStats.
     */
    public void applyPlayerAttributes(Player player, RPGStats stats) {
        if (stats == null) return;

        removeAttributeModifiers(player);

        // 1. VIDA (HEALTH)
        AttributeInstance health = player.getAttribute(HEALTH);
        if (health != null) {
            double rpgMaxHealth = stats.getMaxHealth();

            health.setBaseValue(rpgMaxHealth);

            // Ajuste de vida actual (más robusto)
            if (player.getHealth() > rpgMaxHealth) {
                player.setHealth(rpgMaxHealth);
            }
            // Sincronizar stats.currentHealth si se perdió vida por daño (no por reducción de MaxHP)
            stats.setCurrentHealth(player.getHealth());

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().notifyHealthChange(player);
            }
        }

        // 2. FUERZA (ATTACK_DAMAGE)
        AttributeInstance damage = player.getAttribute(DAMAGE);
        if (damage != null) {
            double baseDamage = 2.0;
            // ************ CORRECCIÓN: Usamos getScaledStrengthBonus() ************
            double addedDamage = stats.getScaledStrengthBonus();

            damage.setBaseValue(baseDamage);

            if (addedDamage > 0.0) { // Usar 0.0 para dobles
                AttributeModifier mod = new AttributeModifier(damageKey, addedDamage, AttributeModifier.Operation.ADD_NUMBER);
                damage.addModifier(mod);
            }
        }

        // 3. MOVIMIENTO (MOVEMENT_SPEED)
        AttributeInstance speed = player.getAttribute(SPEED);
        if (speed != null) {
            double baseSpeed = BASE_MC_SPEED;
            double addedSpeed = stats.getEffectiveMovementSpeed() * SPEED_IMPROVEMENT_PER_POINT;

            speed.setBaseValue(baseSpeed);

            if (addedSpeed > 0.0) { // Usar 0.0 para dobles
                AttributeModifier mod = new AttributeModifier(speedKey, addedSpeed, AttributeModifier.Operation.ADD_NUMBER);
                speed.addModifier(mod);
            }
        }

        // 4. DEFENSA (Notificación)
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().notifyDefenseChange(player);
        }
    }

    public void removeAttributeModifiers(Player player) {
        // ... (el resto del método no requiere cambios) ...
        Collection<NamespacedKey> keys = new ArrayList<>();
        keys.add(healthKey);
        keys.add(damageKey);
        keys.add(speedKey);
        Attribute[] attributes = { HEALTH, DAMAGE, SPEED };
        Set<String> legacyNames = Set.of("rpg_health_mod", "rpg_damage_mod", "rpg_speed_mod");

        for (Attribute attr : attributes) {
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null) continue;

            Collection<AttributeModifier> present = new ArrayList<>(instance.getModifiers());
            for (AttributeModifier mod : present) {
                boolean isRpgMod = false;

                // 1. Comprobación por NamespacedKey
                for (NamespacedKey key : keys) {
                    if (mod.getKey() != null && mod.getKey().equals(key)) {
                        isRpgMod = true;
                        break;
                    }
                }

                // 2. Comprobación por Nombre de legado (backward compatibility)
                if (!isRpgMod && mod.getName() != null && legacyNames.contains(mod.getName())) {
                    isRpgMod = true;
                }

                if (isRpgMod) {
                    instance.removeModifier(mod);
                }
            }

            // CRÍTICO: Restablecer la vida máxima a vanilla para evitar bugs con NMSHealthUtil
            if (attr == HEALTH && instance.getBaseValue() != VANILLA_MAX_HEALTH) {
                instance.setBaseValue(VANILLA_MAX_HEALTH);
            }
        }
    }

    /**
     * MÉTODOS DE FACHADA (CORRECCIÓN): Re-calcula y aplica los atributos del jugador.
     */
    public void recalculateStats(Player player) {
        RPGStats stats = statManager.getStats(player.getUniqueId());

        if (stats == null) {
            plugin.getLogger().warning("No RPGStats encontrado para " + player.getName() + " al recalcular stats.");
            return;
        }

        // NOTA: Es crucial llamar a recalculateEquippedBonuses() antes de applyPlayerAttributes()
        // para que los stats estén actualizados. Aunque esta clase es AttributeApplier,
        // a menudo se encapsula en StatManager para esa lógica. Por ahora, solo usamos los
        // métodos existentes.

        applyPlayerAttributes(player, stats);
    }
}