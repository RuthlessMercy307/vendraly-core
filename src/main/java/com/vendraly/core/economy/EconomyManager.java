package com.vendraly.core.economy;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.auth.AuthManager; // NECESARIO para el caché
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Gestiona el saldo bancario (balance) del jugador.
 * Todas las operaciones se realizan de forma segura, respetando el caché del AuthManager.
 */
public class EconomyManager {

    private final Main plugin;
    private final UserDataManager dataManager;
    private final AuthManager authManager; // Referencia al gestor de estado en memoria

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getUserDataManager();
        // Asume que Main.java tiene un método getAuthManager()
        this.authManager = plugin.getAuthManager();
    }

    // --- UTILITY ---

    private boolean isPlayerOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    /**
     * Obtiene el PlayerData. Si está online, lo obtiene del AuthManager.
     * Si está offline, lo carga del disco (debe ejecutarse en hilo asíncrono).
     */
    private PlayerData getPlayerDataSafe(UUID uuid) throws Exception {
        PlayerData data = authManager.getPlayerData(uuid);
        if (data != null) {
            // Jugador online, usamos el objeto en memoria (acceso en el hilo principal es seguro).
            return data;
        }
        // Jugador offline, cargamos del disco (debe ser asíncrono).
        return dataManager.loadPlayerData(uuid, dataManager.getPlayerName(uuid));
    }

    // =========================================================================
    // MÉTODOS ASÍNCRONOS PRINCIPALES
    // =========================================================================

    /**
     * Obtiene el saldo bancario de un jugador de forma asíncrona.
     * Si está online, lo obtiene directamente del caché (instante).
     * Si está offline, lo obtiene del disco (lento, asíncrono).
     */
    public CompletableFuture<Double> getBalance(UUID uuid) {
        // Si está online, devolvemos el valor instantáneamente del caché (no es I/O)
        if (isPlayerOnline(uuid)) {
            // Aquí usamos el PlayerData que está en el caché
            PlayerData data = authManager.getPlayerData(uuid);
            if (data != null) {
                return CompletableFuture.completedFuture(data.getBalance());
            }
        }

        // Ejecutamos la carga del disco de forma asíncrona (si está offline o no se encontró en caché)
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Usamos la lógica de carga segura
                return getPlayerDataSafe(uuid).getBalance();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al obtener saldo (bank) OFFLINE para " + uuid, e);
                return 0.0;
            }
        });
    }

    /**
     * Añade o resta una cantidad al saldo bancario del jugador de forma asíncrona.
     */
    public CompletableFuture<Boolean> modifyBalance(UUID uuid, double amount) {
        // Ejecutamos en el hilo asíncrono para manejar la lógica de guardado/lectura
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Obtener datos (del caché si online, del disco si offline)
                PlayerData playerData = getPlayerDataSafe(uuid);

                // 2. Aplicar la modificación
                double newBalance = playerData.getBalance() + amount;

                // Validar que no sea negativo
                if (newBalance < 0) {
                    return false;
                }

                // 3. Aplicar cambio al objeto PlayerData
                playerData.setBalance(Math.round(newBalance * 100.0) / 100.0);

                // 4. Guardar los datos
                if (!isPlayerOnline(uuid)) {
                    // Si está OFFLINE, guardamos PlayerData al disco.
                    dataManager.savePlayerData(playerData);
                }
                // Si está ONLINE, el AuthManager guardará el objeto al desconectar,
                // manteniendo la integridad con el resto de los datos (RPG, etc.).

                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al modificar balance para " + uuid + " con cantidad " + amount, e);
                return false;
            }
        });
    }

    /**
     * Transfiere dinero bancario entre jugadores.
     */
    public CompletableFuture<Boolean> transferBalance(UUID sender, UUID recipient, double amount) {
        // Validación básica
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        // 1. Verificar saldo del remitente de forma asíncrona
        return getBalance(sender).thenCompose(senderBalance -> {
            if (senderBalance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            // 2. Restar al remitente, y si tiene éxito (senderSuccess)
            return modifyBalance(sender, -amount)
                    .thenCompose(senderSuccess -> {
                        if (senderSuccess) {
                            // 3. Añadir al receptor
                            return modifyBalance(recipient, amount);
                        } else {
                            // Falla si la resta no fue posible (ej. error de guardado)
                            return CompletableFuture.completedFuture(false);
                        }
                    });
        });
    }
}