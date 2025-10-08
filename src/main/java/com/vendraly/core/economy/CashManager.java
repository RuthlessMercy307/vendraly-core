package com.vendraly.core.economy;

import com.vendraly.VendralyCore;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Maneja el dinero en mano (robable) sincronizado con acciones inmediatas.
 */
public class CashManager {

    private final VendralyCore plugin;
    private final UserDataManager userDataManager;

    public CashManager(VendralyCore plugin, UserDataManager userDataManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
    }

    public double getBalance(UUID uuid) {
        PlayerData data = userDataManager.getOrCreate(uuid, "Desconocido");
        return data.getCashBalance();
    }

    public void give(Player player, double amount) {
        modify(player.getUniqueId(), amount);
        player.sendMessage("Recibiste " + amount + " monedas en efectivo.");
    }

    public boolean take(Player player, double amount) {
        if (getBalance(player.getUniqueId()) < amount) {
            return false;
        }
        modify(player.getUniqueId(), -amount);
        player.sendMessage("Se dedujeron " + amount + " monedas de tu efectivo.");
        return true;
    }

    public boolean transferCash(Player sender, Player target, double amount) {
        if (amount <= 0) {
            return false;
        }
        if (getBalance(sender.getUniqueId()) < amount) {
            return false;
        }
        modify(sender.getUniqueId(), -amount);
        modify(target.getUniqueId(), amount);
        return true;
    }

    public void modify(UUID uuid, double amount) {
        PlayerData data = userDataManager.getOrCreate(uuid, "Desconocido");
        double newBalance = Math.max(0.0, data.getCashBalance() + amount);
        data.setCashBalance(newBalance);
        userDataManager.save(data);
    }
}
