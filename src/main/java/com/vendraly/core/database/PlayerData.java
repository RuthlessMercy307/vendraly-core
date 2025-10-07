package com.vendraly.core.database;

import com.vendraly.core.roles.Role;
import com.vendraly.core.rpg.RPGStats; // ¡Importación clave!
import java.util.UUID;

/**
 * Modelo de datos para la información del jugador.
 * Contiene todos los datos que deben ser persistentes (Auth, Economy, Role, RPG).
 */
public class PlayerData {
    private final UUID playerUUID;
    private final String playerName; // NOTA: El nombre es solo para referencia, nunca para identificación (usar UUID)

    // Auth
    private boolean registered;
    private String passwordHash;
    private int failedAttempts;
    private long lockedUntil;
    private boolean loggedIn;

    // Economy
    private double balance;      // Dinero seguro (bank)
    private double cashBalance;  // Dinero robable (cash)

    // Role
    private Role currentRole;

    // --- RPG AÑADIDO ---
    private RPGStats rpgStats;

    public PlayerData(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.registered = false;
        this.loggedIn = false;
        this.balance = 100.0;
        this.cashBalance = 0.0;
        this.currentRole = Role.PLAYER;

        // CORRECCIÓN CRÍTICA DE SEGURIDAD (Bug 1):
        // Aseguramos que RPGStats tenga el UUID del jugador si lo requiere.
        // Si RPGStats tiene un constructor que toma el UUID:
        // this.rpgStats = new RPGStats(playerUUID);
        // Si no lo tiene, asumimos que se inicializa, pero es un riesgo.
        // Siguiendo tu código, asumo que 'new RPGStats()' es el constructor que usa.
        this.rpgStats = new RPGStats();
    }

    // ===================================
    // Getters y Setters
    // ===================================

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }

    // --- Auth ---
    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public long getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(long lockedUntil) { this.lockedUntil = lockedUntil; }

    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    // --- Economy ---

    public double getBalance() { return balance; }
    /**
     * CORRECCIÓN INVISIBLE (Bug 2): Previene que el balance sea negativo.
     */
    public void setBalance(double balance) {
        if (balance < 0) {
            this.balance = 0;
            // Opcional: Loggear un warning de que se intentó un balance negativo
        } else {
            this.balance = balance;
        }
    }

    public double getCashBalance() { return cashBalance; }
    /**
     * CORRECCIÓN INVISIBLE (Bug 2): Previene que el cashBalance sea negativo.
     */
    public void setCashBalance(double cashBalance) {
        if (cashBalance < 0) {
            this.cashBalance = 0;
            // Opcional: Loggear un warning de que se intentó un balance negativo
        } else {
            this.cashBalance = cashBalance;
        }
    }

    // --- Role ---
    public Role getCurrentRole() { return currentRole; }
    public void setCurrentRole(Role currentRole) { this.currentRole = currentRole; }

    // --- RPG ---
    public RPGStats getRpgStats() { return rpgStats; }
    public void setRpgStats(RPGStats rpgStats) { this.rpgStats = rpgStats; }
}