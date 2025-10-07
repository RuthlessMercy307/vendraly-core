package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.ability.AbilityType;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Núcleo RPGStats: stats infinitos y 100% custom (sin límites vanilla).
 * - Vida/Estamina y progresión
 * - Puntos invertibles
 * - Skills pasivas
 * - Stun/Dodge
 * - Stats extendidos compatibles con UIs previas (movimiento/minado/talado/regen)
 */
public class RPGStats {

    // ----------------------------
    //    Infraestructura básica
    // ----------------------------
    private final Main plugin;
    private final UUID uuid;

    public RPGStats(UUID uuid, Main plugin) {
        this.plugin = plugin;
        this.uuid = uuid;

        // Valores base
        this.maxHealth = BASE_HEALTH;
        this.currentHealth = maxHealth;
        this.maxStamina = BASE_STAMINA_MAX;
        this.currentStamina = maxStamina;

        initializeSkills();
    }

    private void initializeSkills() {
        for (AbilityType type : AbilityType.values()) {
            String key = type.name().toUpperCase();
            skillLevels.putIfAbsent(key, 0);
            skillExp.putIfAbsent(key, 0L);
        }
    }

    // ----------------------------
    //      Progresión principal
    // ----------------------------
    private int level = 1;
    private long totalExp = 0;
    private int unspentPoints = 0;

    public boolean addExp(long amount) {
        this.totalExp += Math.max(0, amount);
        boolean leveledUp = false;

        while (this.totalExp >= getExpForNextLevel(this.level)) {
            this.totalExp -= getExpForNextLevel(this.level);
            this.level++;
            this.unspentPoints++;
            leveledUp = true;
        }
        return leveledUp;
    }

    // Versión estática para compatibilidad con UIs que la llaman de forma estática
    public static long getExpForNextLevel(int lvl) {
        if (lvl < 1) return 100;
        return Math.max(100, Math.round(100 * Math.pow(1.2, lvl - 1)));
    }

    // ----------------------------
    //           Vida / Stamina
    // ----------------------------
    private double currentHealth;
    private double maxHealth;
    private double currentStamina;
    private double maxStamina;

    // Constantes (equilibrio base)
    public static final double BASE_HEALTH = 100.0;
    public static final double BASE_STAMINA_MAX = 100.0;

    public static final double HEALTH_PER_POINT = 10.0;
    public static final double STAMINA_PER_POINT = 20.0;

    public static final double STAMINA_REGEN_PER_POINT = 0.5;  // +0.5/s por punto
    public static final double HEALTH_REGEN_PER_POINT  = 0.25; // +0.25 HP/s por punto

    public double getCurrentHealth() { return currentHealth; }
    public double getMaxHealth() {
        // Vida base + puntos + bonus
        return BASE_HEALTH + (statHealth * HEALTH_PER_POINT) + bonusHealth;
    }
    public void setCurrentHealth(double value) {
        this.currentHealth = clamp(value, 0.0, getMaxHealth());
    }
    public void heal(double amount) { setCurrentHealth(this.currentHealth + Math.max(0, amount)); }
    public void damage(double amount) { setCurrentHealth(this.currentHealth - Math.max(0, amount)); }

    public double getCurrentStamina() { return currentStamina; }
    public double getMaxStamina() {
        // Estamina base + puntos + bonus
        return BASE_STAMINA_MAX + (statStamina * STAMINA_PER_POINT) + bonusStamina;
    }
    public void setCurrentStamina(double value) {
        this.currentStamina = clamp(value, 0.0, getMaxStamina());
    }
    public boolean consumeStamina(double amount) {
        if (amount <= 0) return true;
        if (this.currentStamina >= amount) {
            setCurrentStamina(this.currentStamina - amount);
            return true;
        }
        return false;
    }
    public void restoreStamina(double amount) {
        setCurrentStamina(this.currentStamina + Math.max(0, amount));
    }

    // Regen por segundo (stamina/vida)
    public double getStaminaRegenPerSecond() {
        return 1.0 + (statStaminaRegen * STAMINA_REGEN_PER_POINT);
    }
    public double getHealthRegenPerSecond() {
        // base 0.0 + puntos * factor + BONO (si en el futuro agregas bonus)
        return (statHealthRegen * HEALTH_REGEN_PER_POINT);
    }

    // ----------------------------
    //          Atributos base
    // ----------------------------
    // Puntos invertibles (núcleo)
    private int statStrength = 0;
    private int statDefense  = 0;
    private int statAgility  = 0; // utilitario (robos/interacciones), NO evasión
    private int statHealth   = 0;
    private int statStamina  = 0; // (max stamina)
    private int statStaminaRegen = 0;

    // Stats extendidos (compatibilidad con UIs previas)
    private int statMovementSpeed   = 0; // puntos de velocidad de movimiento (para aplicar en AttributeApplier)
    private int statMiningSpeed     = 0; // recolección: minería
    private int statWoodcuttingSpeed= 0; // recolección: talado
    private int statHealthRegen     = 0; // regen de vida

    // Bonos temporales (equipo/pociones)
    private double bonusStrength = 0;
    private double bonusDefense  = 0;
    private double bonusAgility  = 0;
    private double bonusHealth   = 0;
    private double bonusStamina  = 0;
    // (si quieres: bonusMovementSpeed/mining/woodcutting/healthRegen → se pueden añadir luego)

    // Factores de conversión por punto (daño/defensa). Agilidad no se usa como evasión.
    public static final double STRENGTH_PER_POINT = 1.0; // daño +1 por punto (ejemplo)
    public static final double DEFENSE_PER_POINT  = 0.5; // reducción plana +0.5 por punto
    public static final double AGILITY_PER_POINT  = 1.0; // efectos utilitarios: mostramos “puntos” (no %)

    // Exposición de puntos invertidos (para UIs/menús)
    public int getStatStrength()       { return statStrength; }
    public int getStatDefense()        { return statDefense; }
    public int getStatAgility()        { return statAgility; }
    public int getStatHealth()         { return statHealth; }
    public int getStatStamina()        { return statStamina; }
    public int getStatStaminaRegen()   { return statStaminaRegen; }

    // Compatibilidad: algunos UIs lo piden así por nombre
    public int getStatStaminaMax()     { return statStamina; }
    public int getStatMovementSpeed()  { return statMovementSpeed; }
    public int getStatMiningSpeed()    { return statMiningSpeed; }
    public int getStatWoodcuttingSpeed(){ return statWoodcuttingSpeed; }
    public int getStatHealthRegen()    { return statHealthRegen; }

    // “Effective” (puntos efectivos incluyendo bonos) para mostrar en UI
    public int getEffectiveStrength()  { return statStrength + (int)Math.round(Math.max(0, bonusStrength)); }
    public int getEffectiveDefense()   { return statDefense  + (int)Math.round(Math.max(0, bonusDefense)); }
    public int getEffectiveAgility()   { return statAgility  + (int)Math.round(Math.max(0, bonusAgility)); }
    public int getEffectiveMovementSpeed()   { return statMovementSpeed; }
    public int getEffectiveMiningSpeed()     { return statMiningSpeed; }
    public int getEffectiveWoodcuttingSpeed(){ return statWoodcuttingSpeed; }

    // “Scaled” (valores derivados útiles para cálculos/lore)
    public double getScaledStrengthBonus() { return (statStrength * STRENGTH_PER_POINT) + bonusStrength; }
    public double getScaledDefenseBonus()  { return (statDefense  * DEFENSE_PER_POINT)  + bonusDefense;  }
    public double getScaledAgilityBonus()  { return statAgility + bonusAgility; } // NO % de evasión; solo puntos útiles

    // Potencias derivadas simples (si necesitas cálculos rápidos)
    public double getAttackPower()   { return getScaledStrengthBonus(); }
    public double getDefensePower()  { return getScaledDefenseBonus();  }

    // ----------------------------
    //            Skills pasivas
    // ----------------------------
    private final Map<String, Long>    skillExp    = new HashMap<>();
    private final Map<String, Integer> skillLevels = new HashMap<>();

    public void addSkillExp(String skill, long amount) {
        long curXp = skillExp.getOrDefault(skill, 0L) + Math.max(0, amount);
        int  curLv = skillLevels.getOrDefault(skill, 0);

        long req = getExpForNextLevel(curLv + 1);
        while (curXp >= req) {
            curXp -= req;
            curLv++;
            req = getExpForNextLevel(curLv + 1);
        }
        skillExp.put(skill, curXp);
        skillLevels.put(skill, curLv);
    }

    public int  getSkillLevel(String skill) { return skillLevels.getOrDefault(skill, 0); }
    public long getSkillExp(String skill)   { return skillExp.getOrDefault(skill, 0L); }

    // ----------------------------
    //        Puntos invertibles
    // ----------------------------
    public boolean spendPoint() {
        if (unspentPoints > 0) {
            unspentPoints--;
            return true;
        }
        return false;
    }

    /**
     * Aumenta un stat por nombre (tolerante a variantes).
     * Acepta: strength, defense, agility, health, stamina, staminaregen,
     *         movementspeed, miningspeed, woodcuttingspeed, healthregen
     */
    public boolean increaseStat(String stat) {
        if (!spendPoint()) return false;
        String key = stat.toLowerCase().replace(" ", "");

        switch (key) {
            case "strength"      -> statStrength++;
            case "defense"       -> statDefense++;
            case "agility"       -> statAgility++;
            case "health"        -> { statHealth++;  setCurrentHealth(getMaxHealth()); }
            case "stamina", "staminamax" -> { statStamina++; setCurrentStamina(getMaxStamina()); }
            case "staminaregen"  -> statStaminaRegen++;
            case "movementspeed" -> statMovementSpeed++;
            case "miningspeed"   -> statMiningSpeed++;
            case "woodcuttingspeed" -> statWoodcuttingSpeed++;
            case "healthregen"   -> statHealthRegen++;
            default              -> { unspentPoints++; return false; } // revertir gasto si nombre inválido
        }
        return true;
    }

    // ----------------------------
    //         Bonos temporales
    // ----------------------------
    public void resetBonuses() {
        bonusStrength = bonusDefense = bonusAgility = bonusHealth = bonusStamina = 0.0;
    }
    public void addBonus(String stat, double amount) {
        if (amount == 0) return;
        switch (stat.toLowerCase()) {
            case "strength" -> bonusStrength += amount;
            case "defense"  -> bonusDefense  += amount;
            case "agility"  -> bonusAgility  += amount;
            case "health"   -> { bonusHealth += amount; setCurrentHealth(getCurrentHealth()); }
            case "stamina"  -> { bonusStamina+= amount; setCurrentStamina(getCurrentStamina()); }
        }
    }

    // ----------------------------
    //           Stun / Dodge
    // ----------------------------
    private boolean isStunned = false;
    private boolean dodgeOnCooldown = false;

    // Configura a tu gusto
    private static final double DODGE_COST = 5.0;
    private static final int    DODGE_COOLDOWN_TICKS = 20; // 1s
    public  static final int    STUN_DEFAULT_TICKS = 6;    // ~300ms

    public boolean isStunned() { return isStunned; }

    public void applyStun(int ticks) {
        if (isStunned) return;
        this.isStunned = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> this.isStunned = false, Math.max(1, ticks));
    }

    public boolean tryDodge() {
        if (dodgeOnCooldown) return false;
        if (!consumeStamina(DODGE_COST)) return false;

        dodgeOnCooldown = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> dodgeOnCooldown = false, DODGE_COOLDOWN_TICKS);
        return true;
    }

    public boolean isDodgeAvailable() { return !dodgeOnCooldown; }

    // ----------------------------
    //              Getters
    // ----------------------------
    public UUID getUuid()        { return uuid; }
    public int  getLevel()       { return level; }
    public long getTotalExp()    { return totalExp; }
    public int  getUnspentPoints(){ return unspentPoints; }

    // ----------------------------
    //             Utils
    // ----------------------------
    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max) ? max : v;
    }
}
