package com.vendraly.core.rpg;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Núcleo de stats RPG para entidades (monstruos, bosses, NPC hostiles).
 * Integra vida infinita, escalado exponencial y sincronización opcional con atributos Bukkit.
 */
public class RPGMonster {

    private final LivingEntity entity;
    private final int level;

    // --- Stats RPG ---
    private long currentHealth;
    private long maxHealth;
    private double attackPower;
    private double defensePower;
    private double knockbackResistance;
    private long expReward;
    private double goldReward;

    // --- Config Base ---
    private static final long BASE_HEALTH = 20;
    private static final double BASE_ATTACK = 2.0;
    private static final double BASE_DEFENSE = 0.0;
    private static final long BASE_EXP_REWARD = 10;
    private static final double BASE_GOLD_REWARD = 2.0;

    // --- Escalado ---
    private static final double HP_PER_LEVEL = 3.0;
    private static final double HP_QUAD_FACTOR = 0.5;
    private static final double ATK_PER_LEVEL = 0.5;
    private static final double ATK_QUAD_FACTOR = 0.075;
    private static final double DEF_PER_LEVEL = 0.1;
    private static final double DEF_QUAD_FACTOR = 0.005;
    private static final double EXP_FACTOR = 1.2;
    private static final double GOLD_FACTOR = 1.15;
    private static final double KNOCKBACK_PER_LEVEL = 0.005;

    // --- Opcional ---
    private static final double VARIANCE_FACTOR = 0.15; // ±15% aleatorio en stats

    public RPGMonster(LivingEntity entity, int level) {
        this.entity = entity;
        this.level = Math.max(1, level);

        int diff = this.level - 1;
        long diffSq = (long) diff * diff;

        // Escalado principal
        this.maxHealth = BASE_HEALTH + (long)(diff * HP_PER_LEVEL) + (long)(diffSq * HP_QUAD_FACTOR);
        this.attackPower = BASE_ATTACK + (diff * ATK_PER_LEVEL) + (diffSq * ATK_QUAD_FACTOR);
        this.defensePower = BASE_DEFENSE + (diff * DEF_PER_LEVEL) + (diffSq * DEF_QUAD_FACTOR);
        this.knockbackResistance = Math.min(1.0, diff * KNOCKBACK_PER_LEVEL);
        this.expReward = (long) (BASE_EXP_REWARD * Math.pow(EXP_FACTOR, diff));
        this.goldReward = Math.round(BASE_GOLD_REWARD * Math.pow(GOLD_FACTOR, diff));

        // Variación aleatoria (para evitar mobs clónicos)
        double variance = 1.0 + ((Math.random() * 2 - 1) * VARIANCE_FACTOR);
        this.maxHealth = Math.max(1, (long)(this.maxHealth * variance));
        this.attackPower = Math.max(0.1, this.attackPower * variance);
        this.defensePower = Math.max(0, this.defensePower * variance);

        // Vida actual arranca full
        this.currentHealth = this.maxHealth;
    }

    // ========================
    //      VIDA Y DAÑO
    // ========================
    public long getCurrentHealth() { return currentHealth; }
    public long getMaxHealth() { return maxHealth; }

    public void setCurrentHealth(long value) {
        this.currentHealth = Math.min(Math.max(0, value), maxHealth);
    }

    public void heal(long amount) {
        setCurrentHealth(this.currentHealth + amount);
    }

    public void damage(long amount) {
        setCurrentHealth(this.currentHealth - amount);
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    // ========================
    //       COMBATE
    // ========================
    public double getAttackPower() { return attackPower; }
    public double getDefensePower() { return defensePower; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public long getExpReward() { return expReward; }
    public double getGoldReward() { return goldReward; }

    /**
     * Calcula daño mitigado por defensa RPG.
     */
    public double calculateDamageTaken(double rawDamage) {
        double reduced = rawDamage - defensePower;
        return Math.max(0.1, reduced);
    }

    // ========================
    //     REFERENCIA BUKKIT
    // ========================
    public LivingEntity getEntity() {
        return entity;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Sincroniza atributos vanilla de Bukkit con los valores RPG (opcional).
     * Útil para que el mob se sienta realmente más fuerte (vida, knockback, etc.)
     */
    public void applyAttributes() {
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(maxHealth);
            entity.setHealth(Math.min(maxHealth, currentHealth));
        }

        AttributeInstance knockbackAttr = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(knockbackResistance);
        }

        // NOTA: attackPower y defensePower se usan en DamageEngine, no en atributos vanilla.
    }
}
