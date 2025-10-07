package com.vendraly.core.database;

import com.vendraly.core.Main;
import com.vendraly.core.roles.Role;
import com.vendraly.core.rpg.RPGStats;

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

    // RPG
    private RPGStats rpgStats;

    public PlayerData(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;

        // Defaults seguros
        this.registered = false;
        this.loggedIn = false;
        this.balance = 0.0;
        this.cashBalance = 0.0;
        this.currentRole = Role.PLAYER;

        // RPGStats vinculado al jugador
        Main plugin = new Main();
        this.rpgStats = new RPGStats(playerUUID, plugin);
    }

    // ===================================
    // Getters y Setters
    // ===================================

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    // --- Auth ---
    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = Math.max(0, failedAttempts);
    }

    public long getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(long lockedUntil) {
        this.lockedUntil = Math.max(0, lockedUntil);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    // --- Economy ---
    public double getBalance() {
        return balance;
    }

    /** Previene que el balance sea negativo */
    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
    }

    public double getCashBalance() {
        return cashBalance;
    }

    /** Previene que el cash balance sea negativo */
    public void setCashBalance(double cashBalance) {
        this.cashBalance = Math.max(0, cashBalance);
    }

    // --- Role ---
    public Role getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole != null ? currentRole : Role.PLAYER;
    }

    // --- RPG ---
    public RPGStats getRpgStats() {
        return rpgStats;
    }

    public void setRpgStats(RPGStats rpgStats) {
        if (rpgStats != null) {
            this.rpgStats = rpgStats;
        }
    }
}
