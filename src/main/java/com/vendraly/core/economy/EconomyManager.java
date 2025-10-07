package com.vendraly.core.economy;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.auth.AuthManager;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Gestiona el saldo bancario (balance) del jugador.
 * Todas las operaciones se realizan de forma segura, respetando el caché del AuthManager.
 */
public class EconomyManager {

    private final Main plugin;
    private final UserDataManager dataManager;
    private final AuthManager authManager;

    // Executor dedicado para operaciones de I/O (DB, archivos)
    private final Executor ioExecutor = Executors.newFixedThreadPool(2);

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getUserDataManager();
        this.authManager = plugin.getAuthManager();
    }

    // --- UTILITY ---

    private boolean isPlayerOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    private double round2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Obtiene el PlayerData. Si está online, lo obtiene del AuthManager.
     * Si está offline, lo carga del disco (se espera que se llame de forma async).
     */
    private PlayerData getPlayerDataSafe(UUID uuid) throws Exception {
        PlayerData data = authManager.getPlayerData(uuid);
        if (data != null) return data;

        String playerName = dataManager.getPlayerName(uuid);
        if (playerName == null) throw new IllegalStateException("No se encontró nombre para UUID " + uuid);

        return dataManager.loadPlayerData(uuid, playerName);
    }

    // =========================================================================
    // MÉTODOS ASÍNCRONOS PRINCIPALES
    // =========================================================================

    public CompletableFuture<Double> getBalance(UUID uuid) {
        if (isPlayerOnline(uuid)) {
            PlayerData data = authManager.getPlayerData(uuid);
            if (data != null) {
                return CompletableFuture.completedFuture(data.getBalance());
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerDataSafe(uuid).getBalance();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[Economy] Error al obtener saldo OFFLINE para " + uuid, e);
                return 0.0;
            }
        }, ioExecutor);
    }

    public CompletableFuture<Boolean> modifyBalance(UUID uuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = getPlayerDataSafe(uuid);
                double newBalance = playerData.getBalance() + amount;

                if (newBalance < 0) return false;

                playerData.setBalance(round2Decimals(newBalance));

                if (!isPlayerOnline(uuid)) {
                    dataManager.savePlayerData(playerData);
                }
                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[Economy] Error al modificar balance para " + uuid + " (monto " + amount + ")", e);
                return false;
            }
        }, ioExecutor);
    }

    public CompletableFuture<Boolean> transferBalance(UUID sender, UUID recipient, double amount) {
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
}
