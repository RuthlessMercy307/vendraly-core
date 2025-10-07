package com.vendraly.core.rpg.combat;

import com.vendraly.core.rpg.RPGMonster;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class DamageEngine {

    private static final Random random = new Random();

    // referencia al StatManager (inyectada desde Main)
    private static StatManager statManager;

    public static void init(StatManager manager) {
        statManager = manager;
    }

    // ===========================
    //   CÁLCULO ABSTRACTO
    // ===========================
    public static DamageResult calculateDamage(
            Object attacker, Object defender,
            AttackDirection attackDir,
            AttackDirection defenseDir,
            boolean isBlocking,
            boolean isPerfectBlock,
            boolean isDodging,
            boolean dodgeAvailable) {

        // --- Stats del atacante ---
        double attackPower = 0;
        RPGStats attackerStats = null;

        if (attacker instanceof RPGStats stats) {
            attackPower = stats.getAttackPower();
            attackerStats = stats;
        } else if (attacker instanceof RPGMonster mob) {
            attackPower = mob.getAttackPower();
        }

        // --- Stats del defensor ---
        double defensePower = 0;
        RPGStats defenderStats = null;

        if (defender instanceof RPGStats stats) {
            defensePower = stats.getDefensePower();
            defenderStats = stats;
        } else if (defender instanceof RPGMonster mob) {
            defensePower = mob.getDefensePower();
        }

        // Paso 1: daño base
        double rawDamage = Math.max(1, attackPower - defensePower);

        // Paso 2: esquiva
        if (isDodging && dodgeAvailable && defenderStats != null) {
            defenderStats.consumeStamina(5);
            return new DamageResult(0, "❌ Esquiva perfecta");
        }

        // Paso 3: bloqueo
        if (isBlocking && defenseDir == attackDir) {
            if (isPerfectBlock) {
                if (attackerStats != null) {
                    attackerStats.consumeStamina(10);
                    attackerStats.applyStun(6); // 6 ticks (~300ms)
                }
                return new DamageResult(0, "🛡️ Bloqueo perfecto");
            } else {
                rawDamage *= 0.5;
                if (defenderStats != null) defenderStats.consumeStamina(10);
            }
        }

        // Paso 4: variación ±10%
        rawDamage *= (0.9 + (random.nextDouble() * 0.2));

        return new DamageResult((long) Math.max(1, rawDamage), "⚔️ Golpe normal");
    }

    // ===========================
    //   ATAQUES EN EL MUNDO
    // ===========================
    public static void applyDirectionalAttack(Player attacker, AttackDirection attackDir) {
        double range = 3.0;   // alcance
        double angle = 45.0;  // cono

        for (LivingEntity target : getEntitiesInCone(attacker, range, angle)) {
            RPGStats attackerStats = statManager.getStats(attacker.getUniqueId());
            RPGStats defenderStats = statManager.getStats(target.getUniqueId());

            DamageResult result = calculateDamage(
                    attackerStats, defenderStats,
                    attackDir, AttackDirection.NEUTRAL,
                    false, false, false, false
            );

            if (result.getDamage() > 0) {
                target.damage(result.getDamage(), attacker);
            }

            // Feedback
            attacker.sendMessage(result.getMessage() + " → " + target.getName());
            target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR,
                    target.getLocation().add(0, 1, 0), 5);
        }

        attacker.getWorld().playSound(attacker.getLocation(),
                org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    public static List<LivingEntity> getEntitiesInCone(Player attacker, double range, double angle) {
        Location origin = attacker.getEyeLocation();
        Vector dir = origin.getDirection().normalize();

        return attacker.getWorld().getNearbyEntities(origin, range, range, range).stream()
                .filter(e -> e instanceof LivingEntity && e != attacker)
                .map(e -> (LivingEntity) e)
                .filter(target -> {
                    Vector toEntity = target.getLocation().toVector()
                            .subtract(origin.toVector()).normalize();
                    double degrees = Math.toDegrees(dir.angle(toEntity));
                    return degrees < angle;
                })
                .toList();
    }

    // ===========================
    //   RESULTADO DE COMBATE
    // ===========================
    public static class DamageResult {
        private final long damage;
        private final String message;

        public DamageResult(long damage, String message) {
            this.damage = damage;
            this.message = message;
        }

        public long getDamage() { return damage; }
        public String getMessage() { return message; }
    }
}
