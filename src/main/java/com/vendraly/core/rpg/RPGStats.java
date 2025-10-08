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

    public void addUnspentPoints(int amount) {
        if (amount > 0) this.unspentPoints += amount;
    }

    public int getUnspentPoints() { return unspentPoints; }

    public void setTotalExperience(long exp) {
        this.totalExp = Math.max(0, exp);
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

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

    public static final double BASE_HEALTH = 5.0;
    public static final double BASE_STAMINA_MAX = 100.0;

    public static final double HEALTH_PER_POINT = 1.0;
    public static final double STAMINA_PER_POINT = 10.0;

    public static final double STAMINA_REGEN_PER_POINT = 0.5;
    public static final double HEALTH_REGEN_PER_POINT  = 0.1;

    public double getCurrentHealth() { return currentHealth; }
    public double getMaxHealth() {
        return BASE_HEALTH + (statHealth * HEALTH_PER_POINT) + bonusHealth;
    }
    public void setCurrentHealth(double value) {
        this.currentHealth = clamp(value, 0.0, getMaxHealth());
    }
    public void heal(double amount) { setCurrentHealth(this.currentHealth + Math.max(0, amount)); }
    public void damage(double amount) { setCurrentHealth(this.currentHealth - Math.max(0, amount)); }

    public double getCurrentStamina() { return currentStamina; }
    public double getMaxStamina() {
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

    public double getStaminaRegenPerSecond() {
        return 1.0 + (statStaminaRegen * STAMINA_REGEN_PER_POINT);
    }
    public double getHealthRegenPerSecond() {
        return (statHealthRegen * HEALTH_REGEN_PER_POINT);
    }

    // ----------------------------
    //          Atributos base
    // ----------------------------
    private int statStrength = 0;
    private int statDefense  = 0;
    private int statAgility  = 0;
    private int statHealth   = 0;
    private int statStamina  = 0;
    private int statStaminaRegen = 0;

    private int statMovementSpeed   = 0;
    private int statMiningSpeed     = 0;
    private int statWoodcuttingSpeed= 0;
    private int statHealthRegen     = 0;

    private double bonusStrength = 0;
    private double bonusDefense  = 0;
    private double bonusAgility  = 0;
    private double bonusHealth   = 0;
    private double bonusStamina  = 0;

    public static final double STRENGTH_PER_POINT = 1.0;
    public static final double DEFENSE_PER_POINT  = 0.5;
    public static final double AGILITY_PER_POINT  = 1.0;

    public int getStatStrength()       { return statStrength; }
    public int getStatDefense()        { return statDefense; }
    public int getStatAgility()        { return statAgility; }
    public int getStatHealth()         { return statHealth; }
    public int getStatStamina()        { return statStamina; }
    public int getStatStaminaRegen()   { return statStaminaRegen; }
    public int getStatStaminaMax()     { return statStamina; }
    public int getStatMovementSpeed()  { return statMovementSpeed; }
    public int getStatMiningSpeed()    { return statMiningSpeed; }
    public int getStatWoodcuttingSpeed(){ return statWoodcuttingSpeed; }
    public int getStatHealthRegen()    { return statHealthRegen; }

    public int getEffectiveStrength()  { return statStrength + (int)Math.round(Math.max(0, bonusStrength)); }
    public int getEffectiveDefense()   { return statDefense  + (int)Math.round(Math.max(0, bonusDefense)); }
    public int getEffectiveAgility()   { return statAgility  + (int)Math.round(Math.max(0, bonusAgility)); }

    public double getScaledStrengthBonus() { return (statStrength * STRENGTH_PER_POINT) + bonusStrength; }
    public double getScaledDefenseBonus()  { return (statDefense  * DEFENSE_PER_POINT)  + bonusDefense;  }
    public double getScaledAgilityBonus()  { return statAgility + bonusAgility; }

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

    // ============================================================
    // MÉTODOS PARA AbilityManager (AbilityType)
    // ============================================================
    public int getAbilityLevel(AbilityType ability) {
        if (ability == null) return 0;
        return skillLevels.getOrDefault(ability.name().toUpperCase(), 0);
    }

    public void setAbilityLevel(AbilityType ability, int level) {
        if (ability == null) return;
        skillLevels.put(ability.name().toUpperCase(), Math.max(0, level));
    }

    public long getAbilityExp(AbilityType ability) {
        if (ability == null) return 0L;
        return skillExp.getOrDefault(ability.name().toUpperCase(), 0L);
    }

    public void setAbilityExp(AbilityType ability, long exp) {
        if (ability == null) return;
        skillExp.put(ability.name().toUpperCase(), Math.max(0L, exp));
    }

    public void addAbilityExp(AbilityType ability, long amount) {
        if (ability == null || amount <= 0) return;
        long newExp = getAbilityExp(ability) + amount;
        setAbilityExp(ability, newExp);
    }

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
            default              -> { unspentPoints++; return false; }
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

    private static final double DODGE_COST = 5.0;
    private static final int    DODGE_COOLDOWN_TICKS = 20;
    public  static final int    STUN_DEFAULT_TICKS = 6;

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

    public long getTotalExperience() {
        return getTotalExp();
    }

    // ----------------------------
    //             Utils
    // ----------------------------
    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max) ? max : v;
    }

    // ----------------------------
    //  Bonos de equipo
    // ----------------------------
    private int eqBonusStrength = 0;
    private int eqBonusDefense = 0;
    private int eqBonusMovementSpeed = 0;
    private int eqBonusHealth = 0;
    private int eqBonusStamina = 0;
    private int eqBonusMiningSpeed = 0;
    private int eqBonusWoodcuttingSpeed = 0;
    private int eqBonusHealthRegen = 0;
    private int eqBonusStaminaRegen = 0;

    private final Map<String, Integer> eqBonusSkills = new HashMap<>();

    public void resetEquipmentBonuses() {
        eqBonusStrength = 0;
        eqBonusDefense = 0;
        eqBonusMovementSpeed = 0;
        eqBonusHealth = 0;
        eqBonusStamina = 0;
        eqBonusMiningSpeed = 0;
        eqBonusWoodcuttingSpeed = 0;
        eqBonusHealthRegen = 0;
        eqBonusStaminaRegen = 0;
        eqBonusSkills.clear();
    }

    public void addBonusStrength(int value)        { eqBonusStrength += value; }
    public void addBonusDefense(int value)         { eqBonusDefense += value; }
    public void addBonusMovementSpeed(int value)   { eqBonusMovementSpeed += value; }
    public void addBonusHealth(int value)          { eqBonusHealth += value; }
    public void addBonusStamina(int value)         { eqBonusStamina += value; }
    public void addBonusMiningSpeed(int value)     { eqBonusMiningSpeed += value; }
    public void addBonusWoodcuttingSpeed(int value){ eqBonusWoodcuttingSpeed += value; }
    public void addBonusHealthRegen(int value)     { eqBonusHealthRegen += value; }
    public void addBonusStaminaRegen(int value)    { eqBonusStaminaRegen += value; }
    public void addBonusSkill(String skill, int value) {
        eqBonusSkills.merge(skill.toUpperCase(), value, Integer::sum);
    }

    public int getEffectiveMovementSpeed() {
        return statMovementSpeed + eqBonusMovementSpeed;
    }

    // === Métodos alias para compatibilidad con StatListener ===
    public boolean addExpTotal(long amount) {
        return addExp(amount);
    }

    public int getRpgLevel() {
        return getLevel();
    }

    public float getExpToNextLevel() {
        long needed = getExpForNextLevel(level);
        return (float) ( (double) totalExp / needed ); // progreso 0.0–1.0
    }

    public boolean increaseStatHealth()        { return increaseStat("health"); }
    public boolean increaseStatStrength()      { return increaseStat("strength"); }
    public boolean increaseStatDefense()       { return increaseStat("defense"); }
    public boolean increaseStatMovementSpeed() { return increaseStat("movementspeed"); }
    public boolean increaseStatMiningSpeed()   { return increaseStat("miningspeed"); }
    public boolean increaseStatWoodcuttingSpeed() { return increaseStat("woodcuttingspeed"); }
    public boolean increaseStatHealthRegen()   { return increaseStat("healthregen"); }
    public boolean increaseStatStaminaMax()    { return increaseStat("stamina"); }
    public boolean increaseStatStaminaRegen()  { return increaseStat("staminaregen"); }

    // === Setters usados por UserDataManager ===
    public void setUnspentPoints(int points) {
        this.unspentPoints = Math.max(0, points);
    }

    public void setStatHealth(int value) { this.statHealth = Math.max(0, value); }
    public void setStatStrength(int value) { this.statStrength = Math.max(0, value); }
    public void setStatDefense(int value) { this.statDefense = Math.max(0, value); }
    public void setStatMovementSpeed(int value) { this.statMovementSpeed = Math.max(0, value); }
    public void setStatMiningSpeed(int value) { this.statMiningSpeed = Math.max(0, value); }
    public void setStatWoodcuttingSpeed(int value) { this.statWoodcuttingSpeed = Math.max(0, value); }
    public void setStatHealthRegen(int value) { this.statHealthRegen = Math.max(0, value); }
    public void setStatStaminaMax(int value) { this.statStamina = Math.max(0, value); }
    public void setStatStaminaRegen(int value) { this.statStaminaRegen = Math.max(0, value); }

    // Para compatibilidad con YAML "skills"
    public void setLevel(String skill, int level) {
        skillLevels.put(skill.toUpperCase(), Math.max(0, level));
    }
    public void setExperience(String skill, long exp) {
        skillExp.put(skill.toUpperCase(), Math.max(0L, exp));
    }
    public Map<String, Integer> getSkillLevels() { return new HashMap<>(skillLevels); }
    public Map<String, Long> getSkillExperience() { return new HashMap<>(skillExp); }

    // --- Opcional: maxVanillaLevel si lo usas en XPManager ---
    private int maxVanillaLevelReached = 0;
    public int getMaxVanillaLevelReached() { return maxVanillaLevelReached; }
    public void setMaxVanillaLevelReached(int value) { this.maxVanillaLevelReached = Math.max(0, value); }

    // ============================================================
    //   NUEVOS MÉTODOS ADMIN
    // ============================================================

    /**
     * Modifica un stat de forma administrativa (para comandos).
     */
    public boolean setStatValueAdmin(String statName, int amount, String action) {
        if (statName == null) return false;
        String key = statName.toLowerCase();

        switch (key) {
            case "strength" -> {
                statStrength = applyAction(statStrength, amount, action);
                return true;
            }
            case "defense" -> {
                statDefense = applyAction(statDefense, amount, action);
                return true;
            }
            case "agility" -> {
                statAgility = applyAction(statAgility, amount, action);
                return true;
            }
            case "health" -> {
                statHealth = applyAction(statHealth, amount, action);
                setCurrentHealth(getMaxHealth());
                return true;
            }
            case "stamina" -> {
                statStamina = applyAction(statStamina, amount, action);
                setCurrentStamina(getMaxStamina());
                return true;
            }
            case "staminaregen" -> {
                statStaminaRegen = applyAction(statStaminaRegen, amount, action);
                return true;
            }
            case "movementspeed" -> {
                statMovementSpeed = applyAction(statMovementSpeed, amount, action);
                return true;
            }
            case "miningspeed" -> {
                statMiningSpeed = applyAction(statMiningSpeed, amount, action);
                return true;
            }
            case "woodcuttingspeed" -> {
                statWoodcuttingSpeed = applyAction(statWoodcuttingSpeed, amount, action);
                return true;
            }
            case "healthregen" -> {
                statHealthRegen = applyAction(statHealthRegen, amount, action);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private int applyAction(int current, int amount, String action) {
        return switch (action) {
            case "set" -> Math.max(0, amount);
            case "remove" -> Math.max(0, current - Math.abs(amount));
            default -> Math.max(0, current + amount); // add
        };
    }

    /**
     * Devuelve el valor actual de un objetivo (stat o ability).
     */
    public long getCurrentValueForTarget(String targetType, String targetName) {
        if (targetType == null || targetName == null) return -1;

        switch (targetType.toLowerCase()) {
            case "stat" -> {
                return switch (targetName.toLowerCase()) {
                    case "strength" -> statStrength;
                    case "defense" -> statDefense;
                    case "agility" -> statAgility;
                    case "health" -> statHealth;
                    case "stamina" -> statStamina;
                    case "staminaregen" -> statStaminaRegen;
                    case "movementspeed" -> statMovementSpeed;
                    case "miningspeed" -> statMiningSpeed;
                    case "woodcuttingspeed" -> statWoodcuttingSpeed;
                    case "healthregen" -> statHealthRegen;
                    default -> -1;
                };
            }
            case "ability" -> {
                try {
                    AbilityType type = AbilityType.valueOf(targetName.toUpperCase());
                    return getAbilityLevel(type);
                } catch (IllegalArgumentException e) {
                    return -1;
                }
            }
            default -> {
                return -1;
            }
        }
    }
}
