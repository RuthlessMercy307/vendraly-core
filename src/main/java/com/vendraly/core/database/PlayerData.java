package com.vendraly.core.database;

import com.vendraly.core.jobs.JobProgress;
import com.vendraly.core.roles.Role;
import com.vendraly.core.rpg.stats.RPGStats;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representa la información persistente de un jugador.
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private String passwordHash;
    private boolean authenticated;
    private Role role;
    private double bankBalance;
    private double cashBalance;
    private final RPGStats stats;
    private final Map<String, JobProgress> jobs;
    private String clanId;
    private int rpgLevel;
    private long rpgExperience;
    private int unspentPoints;
    private boolean banned;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.stats = new RPGStats();
        this.jobs = new HashMap<>();
        this.role = Role.CIVILIAN;
        this.passwordHash = "";
        this.bankBalance = 0.0D;
        this.cashBalance = 0.0D;
        this.rpgLevel = 1;
        this.rpgExperience = 0L;
        this.unspentPoints = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public RPGStats getStats() {
        return stats;
    }

    public Map<String, JobProgress> getJobs() {
        return jobs;
    }

    public String getClanId() {
        return clanId;
    }

    public void setClanId(String clanId) {
        this.clanId = clanId;
    }

    public int getRpgLevel() {
        return rpgLevel;
    }

    public void setRpgLevel(int rpgLevel) {
        this.rpgLevel = rpgLevel;
    }

    public long getRpgExperience() {
        return rpgExperience;
    }

    public void setRpgExperience(long rpgExperience) {
        this.rpgExperience = rpgExperience;
    }

    public int getUnspentPoints() {
        return unspentPoints;
    }

    public void setUnspentPoints(int unspentPoints) {
        this.unspentPoints = unspentPoints;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public JobProgress getOrCreateJob(String id) {
        return jobs.computeIfAbsent(id.toLowerCase(), key -> new JobProgress(key));
    }

    public void resetAuth() {
        this.authenticated = false;
    }

    public void applyOnlineName() {
        if (Bukkit.getPlayer(uuid) != null) {
            Bukkit.getPlayer(uuid).setDisplayName(role.getPrefix() + name);
        }
    }
}
