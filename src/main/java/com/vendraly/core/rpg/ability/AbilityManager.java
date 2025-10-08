package com.vendraly.core.rpg.ability;

import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stats.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gestiona habilidades activas y pasivas de los jugadores.
 */
public class AbilityManager {

    private final StatManager statManager;
    private final Map<UUID, Set<AbilityType>> unlocked = new HashMap<>();
    private final Map<UUID, Map<AbilityType, Long>> cooldowns = new HashMap<>();

    public AbilityManager(StatManager statManager) {
        this.statManager = statManager;
    }

    public void unlock(Player player, AbilityType ability) {
        unlocked.computeIfAbsent(player.getUniqueId(), id -> EnumSet.noneOf(AbilityType.class)).add(ability);
        player.sendMessage(Component.text("Desbloqueaste la habilidad " + ability.name(), NamedTextColor.LIGHT_PURPLE));
    }

    public boolean hasAbility(Player player, AbilityType ability) {
        return unlocked.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(ability);
    }

    public boolean trigger(Player player, AbilityType ability) {
        if (!hasAbility(player, ability)) {
            player.sendMessage(Component.text("No conoces esa habilidad", NamedTextColor.RED));
            return false;
        }
        long now = System.currentTimeMillis();
        long next = cooldowns.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(AbilityType.class))
                .getOrDefault(ability, 0L);
        if (now < next) {
            player.sendMessage(Component.text("Habilidad en enfriamiento", NamedTextColor.RED));
            return false;
        }
        applyEffect(player, ability);
        cooldowns.get(player.getUniqueId()).put(ability, now + 10000L);
        return true;
    }

    private void applyEffect(Player player, AbilityType ability) {
        switch (ability) {
            case BERSERK -> {
                statManager.rewardAction(player, StatType.STRENGTH, 1.5, 20);
                player.sendMessage(Component.text("Activaste Berserk!", NamedTextColor.DARK_RED));
            }
            case DODGE -> player.sendMessage(Component.text("Tu agilidad aumenta temporalmente", NamedTextColor.GREEN));
            case HEALING_AURA -> player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 4));
            case FIREBALL -> player.launchProjectile(org.bukkit.entity.SmallFireball.class);
        }
    }

    public void tick() {
        // Enfriamientos automáticos: limpiar habilidades expiradas
        long now = System.currentTimeMillis();
        for (Map<AbilityType, Long> map : cooldowns.values()) {
            map.entrySet().removeIf(entry -> entry.getValue() < now);
        }
    }
}
