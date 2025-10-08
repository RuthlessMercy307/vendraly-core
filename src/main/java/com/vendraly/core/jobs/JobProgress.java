package com.vendraly.core.jobs;

/**
 * Representa la progresión de un jugador en un oficio concreto.
 */
public class JobProgress {

    private final String jobId;
    private long experience;
    private int level;

    public JobProgress(String jobId) {
        this.jobId = jobId;
        this.level = 1;
    }

    public String getJobId() {
        return jobId;
    }

    public long getExperience() {
        return experience;
    }

    public void addExperience(long amount) {
        this.experience += amount;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
