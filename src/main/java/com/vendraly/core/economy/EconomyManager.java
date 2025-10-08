package com.vendraly.core.economy;

import com.vendraly.VendralyCore;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Maneja la economía bancaria (saldo seguro) de los jugadores.
 */
public class EconomyManager {

    private final VendralyCore plugin;
    private final UserDataManager userDataManager;

    public EconomyManager(VendralyCore plugin, UserDataManager userDataManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
    }

    public double getBalance(OfflinePlayer player) {
        PlayerData data = userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        return data.getBankBalance();
    }

    public void deposit(UUID uuid, double amount) {
        modify(uuid, amount);
    }

    public void withdraw(UUID uuid, double amount) {
        modify(uuid, -amount);
    }

    public void setBalance(UUID uuid, double amount) {
        PlayerData data = userDataManager.getOrCreate(uuid, "Desconocido");
        data.setBankBalance(Math.max(0.0, amount));
        userDataManager.save(data);
    }

    public boolean transfer(UUID sender, UUID target, double amount) {
        if (amount <= 0) {
            return false;
        }
        PlayerData senderData = userDataManager.getOrCreate(sender, "Desconocido");
        if (senderData.getBankBalance() < amount) {
            return false;
        }
        PlayerData targetData = userDataManager.getOrCreate(target, "Desconocido");
        senderData.setBankBalance(senderData.getBankBalance() - amount);
        targetData.setBankBalance(targetData.getBankBalance() + amount);
        userDataManager.save(senderData);
        userDataManager.save(targetData);
        return true;
    }

    private void modify(UUID uuid, double amount) {
        PlayerData data = userDataManager.getOrCreate(uuid, "Desconocido");
        double newBalance = Math.max(0.0, data.getBankBalance() + amount);
        data.setBankBalance(newBalance);
        userDataManager.save(data);
    }
}
