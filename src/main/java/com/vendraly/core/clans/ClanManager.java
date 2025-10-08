package com.vendraly.core.clans;

import com.vendraly.core.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la vida de los clanes, invitaciones y guerras.
 */
public class ClanManager {

    private final ConfigManager configManager;
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerClan = new ConcurrentHashMap<>();
    private final Set<String> wars = ConcurrentHashMap.newKeySet();

    public ClanManager(ConfigManager configManager) {
        this.configManager = configManager;
        load();
    }

    public Optional<Clan> getClan(String id) {
        return Optional.ofNullable(clans.get(id.toLowerCase()));
    }

    public Clan createClan(Player creator, String id, String name) {
        String key = id.toLowerCase();
        Clan clan = new Clan(key, name, creator.getUniqueId());
        clans.put(key, clan);
        playerClan.put(creator.getUniqueId(), key);
        save();
        broadcast(Component.text(creator.getName() + " fundó el clan " + name, NamedTextColor.GOLD));
        return clan;
    }

    public boolean invite(Player inviter, Player invited) {
        Clan clan = getClanByPlayer(inviter.getUniqueId());
        if (clan == null) {
            return false;
        }
        clan.getInvites().add(invited.getUniqueId());
        invited.sendMessage("Has sido invitado al clan " + clan.getName());
        return true;
    }

    public boolean join(Player player, String id) {
        Clan clan = clans.get(id.toLowerCase());
        if (clan == null) {
            return false;
        }
        if (!clan.getInvites().contains(player.getUniqueId())) {
            return false;
        }
        clan.getInvites().remove(player.getUniqueId());
        clan.getMembers().add(player.getUniqueId());
        playerClan.put(player.getUniqueId(), clan.getId());
        save();
        broadcast(Component.text(player.getName() + " se unió al clan " + clan.getName(), NamedTextColor.GREEN));
        return true;
    }

    public void leave(Player player) {
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            return;
        }
        clan.getMembers().remove(player.getUniqueId());
        playerClan.remove(player.getUniqueId());
        save();
        broadcast(Component.text(player.getName() + " abandonó el clan " + clan.getName(), NamedTextColor.YELLOW));
    }

    public Clan getClanByPlayer(UUID uuid) {
        String id = playerClan.get(uuid);
        if (id == null) {
            return null;
        }
        return clans.get(id);
    }

    public void declareWar(String clanA, String clanB) {
        String key = clanA.toLowerCase() + ":" + clanB.toLowerCase();
        wars.add(key);
        broadcast(Component.text("¡Guerra declarada entre " + clanA + " y " + clanB + "!", NamedTextColor.DARK_RED));
    }

    public boolean isAtWar(String clanA, String clanB) {
        return wars.contains(clanA.toLowerCase() + ":" + clanB.toLowerCase()) || wars.contains(clanB.toLowerCase() + ":" + clanA.toLowerCase());
    }

    public void endWar(String clanA, String clanB) {
        wars.remove(clanA.toLowerCase() + ":" + clanB.toLowerCase());
        wars.remove(clanB.toLowerCase() + ":" + clanA.toLowerCase());
        broadcast(Component.text("La guerra entre " + clanA + " y " + clanB + " ha terminado.", NamedTextColor.GRAY));
    }

    private void broadcast(Component component) {
        Bukkit.getServer().broadcast(component);
    }

    private void load() {
        FileConfiguration config = configManager.get("clans.yml");
        if (!config.isConfigurationSection("clans")) {
            return;
        }
        for (String key : config.getConfigurationSection("clans").getKeys(false)) {
            String name = config.getString("clans." + key + ".name", key);
            UUID leader = UUID.fromString(config.getString("clans." + key + ".leader"));
            Clan clan = new Clan(key, name, leader);
            List<String> members = config.getStringList("clans." + key + ".members");
            for (String member : members) {
                UUID uuid = UUID.fromString(member);
                clan.getMembers().add(uuid);
                playerClan.put(uuid, key);
            }
            clans.put(key, clan);
        }
    }

    public void save() {
        FileConfiguration config = configManager.get("clans.yml");
        config.set("clans", null);
        for (Clan clan : clans.values()) {
            String path = "clans." + clan.getId();
            config.set(path + ".name", clan.getName());
            config.set(path + ".leader", clan.getLeader().toString());
            config.set(path + ".members", clan.getMembers().stream().map(UUID::toString).toList());
        }
        configManager.save("clans.yml");
    }
}
