package com.vendraly.core.rpg.combat;

import com.vendraly.VendralyCore;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.rpg.ability.AbilityManager;
import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stamina.StaminaManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coordina entradas de combate y estados direccionales.
 */
public class CombatManager {

    private final VendralyCore plugin;
    private final StatManager statManager;
    private final AbilityManager abilityManager;
    private final StaminaManager staminaManager;
    private final DamageEngine damageEngine;
    private final Map<UUID, AttackDirection> currentDirections = new HashMap<>();

    public CombatManager(VendralyCore plugin, StatManager statManager, AbilityManager abilityManager, StaminaManager staminaManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.abilityManager = abilityManager;
        this.staminaManager = staminaManager;
        this.damageEngine = new DamageEngine(plugin.getUserDataManager());
    }

    public void setDirection(Player player, AttackDirection direction) {
        currentDirections.put(player.getUniqueId(), direction);
    }

    public AttackDirection getDirection(Player player) {
        return currentDirections.getOrDefault(player.getUniqueId(), AttackDirection.NORTH);
    }

    public double computeDamage(Player attacker) {
        AttackDirection direction = getDirection(attacker);
        return damageEngine.computeDamage(attacker, direction);
    }

    public double computeDefense(Player defender) {
        AttackDirection direction = getDirection(defender);
        return damageEngine.computeDefense(defender, direction);
    }

    public boolean consumeStamina(Player player, double amount) {
        return staminaManager.consume(player, amount);
    }

    public void reset(Player player) {
        currentDirections.remove(player.getUniqueId());
    }
}
