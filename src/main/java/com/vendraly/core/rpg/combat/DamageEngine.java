package com.vendraly.core.rpg.combat;

import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.rpg.stats.StatType;
import org.bukkit.entity.Player;

/**
 * Calcula el daño final basado en estadísticas personalizadas.
 */
public class DamageEngine {

    private final UserDataManager userDataManager;

    public DamageEngine(UserDataManager userDataManager) {
        this.userDataManager = userDataManager;
    }

    public double computeDamage(Player attacker, AttackDirection direction) {
        PlayerData data = userDataManager.getOrCreate(attacker.getUniqueId(), attacker.getName());
        double base = data.getStats().getStat(StatType.STRENGTH);
        double agility = data.getStats().getStat(StatType.AGILITY);
        double modifier = switch (direction) {
            case NORTH, SOUTH -> 1.0;
            case EAST, WEST -> 1.1;
            default -> 1.05;
        };
        double critChance = Math.min(50.0, agility / 2);
        if (Math.random() * 100 < critChance) {
            modifier *= 1.5;
        }
        return base * modifier;
    }

    public double computeDefense(Player defender, AttackDirection direction) {
        PlayerData data = userDataManager.getOrCreate(defender.getUniqueId(), defender.getName());
        double defense = data.getStats().getStat(StatType.DEFENSE);
        double resilience = data.getStats().getStat(StatType.RESILIENCE);
        double modifier = direction == AttackDirection.NORTH ? 1.2 : 1.0;
        return defense * modifier + resilience;
    }
}
