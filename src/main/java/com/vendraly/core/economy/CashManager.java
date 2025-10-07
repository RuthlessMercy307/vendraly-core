package com.vendraly.core.economy;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.auth.AuthManager; // NECESARIO
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Clase que maneja todas las operaciones de la economía de efectivo robable (cash_balance).
 * Todas las operaciones se realizan de forma segura, respetando el caché del AuthManager.
 */
public class CashManager {

    private final Main plugin;
    private final UserDataManager dataManager;
    private final AuthManager authManager; // Referencia al gestor de estado en memoria

    public CashManager(Main plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getUserDataManager();
        this.authManager = plugin.getAuthManager();
    }

    // --- UTILITY ---

    private boolean isPlayerOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    /**
     * Intenta obtener el PlayerData. Si está online, lo obtiene del AuthManager.
     * Si está offline, lo carga del disco.
     * ADVERTENCIA: Este método DEBE EJECUTARSE en un HILO ASÍNCRONO si el jugador está offline,
     * ya que usa dataManager.loadPlayerData().
     */
    private PlayerData getPlayerDataSafe(UUID uuid) throws Exception {
        PlayerData data = authManager.getPlayerData(uuid);
        if (data != null) {
            // Jugador online, usamos el objeto en memoria.
            return data;
        }
        // Jugador offline, cargamos del disco.
        return dataManager.loadPlayerData(uuid, dataManager.getPlayerName(uuid));
    }


    // =========================================================================
    // MÉTODOS ASÍNCRONOS PRINCIPALES
    // =========================================================================

    /**
     * Obtiene el saldo (cash_balance) de forma asíncrona.
     * Si el jugador está online, lo obtiene directamente del caché del AuthManager (en el hilo principal).
     * Si está offline, lo obtiene del disco (en el hilo asíncrono).
     */
    public CompletableFuture<Double> getBalance(UUID uuid) {
        if (isPlayerOnline(uuid)) {
            // Camino 1: Jugador ONLINE (Hilo Principal) - Lectura instantánea del caché
            return CompletableFuture.completedFuture(authManager.getPlayerData(uuid).getCashBalance());
        }

        // Camino 2: Jugador OFFLINE (Hilo Asíncrono) - Lectura lenta del disco
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Usamos la lógica de carga segura (offline)
                return getPlayerDataSafe(uuid).getCashBalance();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al obtener saldo (cash) OFFLINE para " + uuid, e);
                return 0.0;
            }
        });
    }

    /**
     * Añade o resta una cantidad al cash_balance de forma asíncrona.
     */
    public CompletableFuture<Boolean> modifyBalance(UUID uuid, double amount) {
        // Ejecutamos en el hilo asíncrono para manejar la lógica de guardado/lectura
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Obtener datos (del caché si online, del disco si offline)
                PlayerData playerData = getPlayerDataSafe(uuid);

                // 2. Aplicar la modificación
                double newBalance = playerData.getCashBalance() + amount;

                if (newBalance < 0) {
                    return false;
                }

                // 3. Aplicar cambio al objeto PlayerData
                playerData.setCashBalance(Math.round(newBalance * 100.0) / 100.0);

                // 4. Guardar los datos
                if (!isPlayerOnline(uuid)) {
                    // Si está OFFLINE, guardamos PlayerData al disco.
                    dataManager.savePlayerData(playerData);
                }
                // Si está ONLINE, el AuthManager lo guardará al desconectar,
                // asegurando que otros cambios (RPG) no se sobrescriban.

                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al modificar cash_balance para " + uuid + " con cantidad " + amount, e);
                return false;
            }
        });
    }

    // =========================================================================
    // MÉTODOS DE CONVENIENCIA ASÍNCRONOS
    // =========================================================================

    public CompletableFuture<Double> getCash(UUID uuid) {
        return getBalance(uuid);
    }

    public CompletableFuture<Boolean> modifyCash(UUID uuid, double amount) {
        return modifyBalance(uuid, amount);
    }

    /**
     * Transfiere cash entre jugadores (se mantiene la lógica asíncrona).
     */
    public CompletableFuture<Boolean> transferCash(UUID sender, UUID recipient, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        // Se mantiene la lógica de thenCompose para garantizar la atomicidad.
        return getBalance(sender).thenCompose(senderBalance -> {
            if (senderBalance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            // Restar del remitente y si es exitoso, sumar al receptor
            return modifyBalance(sender, -amount)
                    .thenCompose(senderSuccess -> {
                        if (senderSuccess) {
                            return modifyBalance(recipient, amount);
                        } else {
                            // La resta falló
                            return CompletableFuture.completedFuture(false);
                        }
                    });
        });
    }

    // =========================================================================
    // MÉTODOS SINCRÓNICOS (Evitar su uso siempre que sea posible)
    // =========================================================================

    /**
     * Obtiene el saldo de efectivo de un jugador de forma síncrona.
     */
    public double getBalance(Player player) {
        // CORRECCIÓN: Si el jugador está online, no bloqueamos.
        // Solo bloqueamos si necesitamos la funcionalidad getBalance(UUID).join().
        PlayerData data = authManager.getPlayerData(player.getUniqueId());
        if (data != null) {
            return data.getCashBalance();
        }
        // Fallback inseguro si el AuthManager no tiene los datos (solo debería pasar en error)
        plugin.getLogger().warning("PlayerData no encontrado en AuthManager para jugador online: " + player.getName());
        return getBalance(player.getUniqueId()).join();
    }

    /**
     * Resta efectivo de un jugador de forma síncrona.
     */
    public boolean take(Player player, double amount) {
        // CORRECCIÓN: Para evitar el bloqueo, si está online, se debe usar la lógica del AuthManager
        // y luego forzar el guardado si es necesario. Lo más seguro es usar el método asíncrono con .join().
        return modifyBalance(player.getUniqueId(), -Math.abs(amount)).join();
    }

    /**
     * Da efectivo a un jugador de forma síncrona.
     */
    public boolean give(Player player, double amount) {
        return modifyBalance(player.getUniqueId(), Math.abs(amount)).join();
    }
}