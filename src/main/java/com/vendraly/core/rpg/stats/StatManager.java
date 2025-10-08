package com.vendraly.core.rpg.stats;

import com.vendraly.VendralyCore;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Coordina la lógica de estadísticas RPG, aplicando sus efectos en el juego.
 */
public class StatManager {

    private final VendralyCore plugin;
    private final UserDataManager userDataManager;
    private final XPManager xpManager;

    public StatManager(VendralyCore plugin, UserDataManager userDataManager, XPManager xpManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
        this.xpManager = xpManager;
    }

    public void apply(Player player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        Map<StatType, Double> stats = new EnumMap<>(data.getStats().asMap());
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(stats.getOrDefault(StatType.HEALTH, StatType.HEALTH.getBaseValue()));
            player.setHealth(Math.min(player.getHealth(), maxHealth.getBaseValue()));
        }
        AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(0.1 + stats.getOrDefault(StatType.AGILITY, 0.0) / 100.0);
        }
        AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(1.0 + stats.getOrDefault(StatType.STRENGTH, 0.0) / 5.0);
        }
    }

    public void addStatPoint(Player player, StatType type) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        if (data.getUnspentPoints() <= 0) {
            player.sendMessage(Component.text("No tienes puntos disponibles", NamedTextColor.RED));
            return;
        }
        data.setUnspentPoints(data.getUnspentPoints() - 1);
        data.getStats().addStat(type, 1);
        userDataManager.save(data);
        apply(player);
        player.sendMessage(Component.text("Invertiste un punto en " + type.name().toLowerCase(), NamedTextColor.GREEN));
    }

    public void resetPoints(Player player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        int totalPoints = (data.getRpgLevel() - 1) * 5;
        data.setUnspentPoints(totalPoints);
        for (StatType type : StatType.values()) {
            data.getStats().setStat(type, type.getBaseValue());
        }
        userDataManager.save(data);
        apply(player);
        player.sendMessage(Component.text("Se han reiniciado tus estadísticas", NamedTextColor.YELLOW));
    }

    public int getLevel(Player player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        return data.getRpgLevel();
    }

    public long getExperience(Player player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        return data.getRpgExperience();
    }

    public void rewardAction(Player player, StatType type, double amount, long xp) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        data.getStats().addStat(type, amount);
        userDataManager.save(data);
        xpManager.addExperience(player, xp);
        player.sendMessage(Component.text("Tus habilidades en " + type.name().toLowerCase() + " han aumentado", NamedTextColor.AQUA));
    }
}
