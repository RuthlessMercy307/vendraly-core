package com.vendraly.core.protection;

import com.vendraly.core.clans.Clan;
import com.vendraly.core.clans.ClanManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona protecciones de cofres y regiones asociadas a clanes.
 */
public class ProtectionManager {

    private final ClanManager clanManager;
    private final Map<Location, UUID> protectedChests = new HashMap<>();

    public ProtectionManager(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean canAccessChest(Player player, Location location) {
        if (location == null) {
            return true;
        }
        UUID owner = protectedChests.get(location);
        if (owner == null) {
            return true;
        }
        if (owner.equals(player.getUniqueId())) {
            return true;
        }
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan != null && clan.getMembers().contains(owner)) {
            return true;
        }
        return false;
    }

    public void protectChest(Player player, Location location) {
        protectedChests.put(location, player.getUniqueId());
    }

    public double getCurrencyValue(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        if (itemStack.getType() == Material.GOLD_INGOT) {
            return itemStack.getAmount() * 10.0;
        }
        if (itemStack.getType() == Material.DIAMOND) {
            return itemStack.getAmount() * 25.0;
        }
        return 0;
    }
}
