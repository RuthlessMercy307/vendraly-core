package com.vendraly.core.clans;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Representa un clan persistente.
 */
public class Clan {

    private final String id;
    private final String name;
    private final UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> invites = new HashSet<>();

    public Clan(String id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getInvites() {
        return invites;
    }
}
