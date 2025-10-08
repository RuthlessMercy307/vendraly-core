package com.vendraly.core.rpg.stats;

import com.vendraly.VendralyCore;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Gestiona la experiencia y niveles RPG de los jugadores.
 */
public class XPManager {

    private final VendralyCore plugin;
    private final UserDataManager userDataManager;

    public XPManager(VendralyCore plugin, UserDataManager userDataManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
    }

    public void addExperience(Player player, long amount) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        long newXp = data.getRpgExperience() + amount;
        data.setRpgExperience(newXp);
        while (newXp >= getRequiredForLevel(data.getRpgLevel() + 1)) {
            levelUp(player, data);
        }
        userDataManager.save(data);
    }

    public void addExperience(UUID uuid, long amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            addExperience(player, amount);
        } else {
            PlayerData data = userDataManager.getOrCreate(uuid, "Desconocido");
            long newXp = data.getRpgExperience() + amount;
            data.setRpgExperience(newXp);
            while (newXp >= getRequiredForLevel(data.getRpgLevel() + 1)) {
                data.setRpgLevel(data.getRpgLevel() + 1);
                data.setUnspentPoints(data.getUnspentPoints() + 5);
            }
            userDataManager.save(data);
        }
    }

    private void levelUp(Player player, PlayerData data) {
        data.setRpgLevel(data.getRpgLevel() + 1);
        data.setUnspentPoints(data.getUnspentPoints() + 5);
        player.sendMessage(Component.text("¡Has subido a nivel " + data.getRpgLevel() + "!", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), "entity.player.levelup", 1f, 1.2f);
    }

    public long getRequiredForLevel(int level) {
        return Math.round(Math.pow(level, 2.5) * 50);
    }

    public int getUnspentPoints(Player player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        return data.getUnspentPoints();
    }

    public void consumePoints(Player player, int amount) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        data.setUnspentPoints(Math.max(0, data.getUnspentPoints() - amount));
        userDataManager.save(data);
    }
}
