package com.vendraly.core.jobs;

import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa un oficio con materiales relacionados.
 */
public class JobDefinition {

    private final String id;
    private final String displayName;
    private final Set<Material> materials = new HashSet<>();
    private final long baseReward;

    public JobDefinition(String id, String displayName, long baseReward) {
        this.id = id;
        this.displayName = displayName;
        this.baseReward = baseReward;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getBaseReward() {
        return baseReward;
    }

    public Set<Material> getMaterials() {
        return materials;
    }
}
