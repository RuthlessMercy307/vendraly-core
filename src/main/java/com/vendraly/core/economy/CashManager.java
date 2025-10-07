package com.vendraly.core.economy;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.auth.AuthManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Maneja todas las operaciones de la economía de efectivo robable (cash_balance).
 * Usa caché (AuthManager) para jugadores online y persistencia para offline.
 */
public class CashManager {

    private final Main plugin;
    private final UserDataManager dataManager;
    private final AuthManager authManager;

    // Executor dedicado para operaciones asíncronas (2 hilos I/O)
    private final Executor ioExecutor = Executors.newFixedThreadPool(2);

    public CashManager(Main plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getUserDataManager();
        this.authManager = plugin.getAuthManager();
    }

    // --- UTILS ---

    private boolean isPlayerOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    private double round2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private PlayerData getPlayerDataSafe(UUID uuid) throws Exception {
        PlayerData data = authManager.getPlayerData(uuid);
        if (data != null) return data;

        String name = dataManager.getPlayerName(uuid);
        if (name == null) throw new IllegalStateException("No se encontró nombre para UUID " + uuid);

        return dataManager.loadPlayerData(uuid, name);
    }

    // =========================================================================
    // ASYNC
    // =========================================================================

    public CompletableFuture<Double> getBalance(UUID uuid) {
        if (isPlayerOnline(uuid)) {
            PlayerData data = authManager.getPlayerData(uuid);
            if (data != null) {
                return CompletableFuture.completedFuture(data.getCashBalance());
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerDataSafe(uuid).getCashBalance();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[CashManager] Error al obtener cash OFFLINE para " + uuid, e);
                return 0.0;
            }
        }, ioExecutor);
    }

    public CompletableFuture<Boolean> modifyBalance(UUID uuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = getPlayerDataSafe(uuid);
                double newBalance = playerData.getCashBalance() + amount;

                if (newBalance < 0) return false;

                playerData.setCashBalance(round2Decimals(newBalance));

                if (!isPlayerOnline(uuid)) {
                    dataManager.savePlayerData(playerData);
                }
                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[CashManager] Error al modificar cash para " + uuid + " (monto " + amount + ")", e);
                return false;
            }
        }, ioExecutor);
    }

    public CompletableFuture<Boolean> transferCash(UUID sender, UUID recipient, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getBalance(sender).thenCompose(senderBalance -> {
            if (senderBalance < amount) return CompletableFuture.completedFuture(false);

            return modifyBalance(sender, -amount).thenCompose(senderSuccess -> {
                if (senderSuccess) {
                    return modifyBalance(recipient, amount);
                }
                return CompletableFuture.completedFuture(false);
            });
        });
    }

    // =========================================================================
    // SYNC (Úsese con cuidado)
    // =========================================================================

    public double getBalance(Player player) {
        PlayerData data = authManager.getPlayerData(player.getUniqueId());
        if (data != null) {
            return data.getCashBalance();
        }
        plugin.getLogger().warning("[CashManager] PlayerData no encontrado en AuthManager para online: " + player.getName());
        return getBalance(player.getUniqueId()).join(); // ⚠️ join bloquea si está offline
    }

    public boolean take(Player player, double amount) {
        return modifyBalance(player.getUniqueId(), -Math.abs(amount)).join();
    }

    public boolean give(Player player, double amount) {
        return modifyBalance(player.getUniqueId(), Math.abs(amount)).join();
    }
}
