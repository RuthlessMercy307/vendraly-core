package com.vendraly.core.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Representa una sesión de trade entre dos jugadores.
 */
public class TradeSession {

    private final Player requester;
    private final Player target;
    private final Inventory inventory;
    private boolean requesterReady;
    private boolean targetReady;
    private double requesterMoney;
    private double targetMoney;

    public TradeSession(Player requester, Player target, Inventory inventory) {
        this.requester = requester;
        this.target = target;
        this.inventory = inventory;
    }

    public Player getRequester() {
        return requester;
    }

    public Player getTarget() {
        return target;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int[] getRequesterSlots() {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    }

    public int[] getTargetSlots() {
        return new int[]{27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    }

    public void toggleReady(Player player) {
        if (player.equals(requester)) {
            requesterReady = !requesterReady;
        } else if (player.equals(target)) {
            targetReady = !targetReady;
        }
    }

    public boolean isReady() {
        return requesterReady && targetReady;
    }

    public void resetReady() {
        requesterReady = false;
        targetReady = false;
    }

    public void setMoney(Player player, double amount) {
        if (player.equals(requester)) {
            requesterMoney = amount;
        } else if (player.equals(target)) {
            targetMoney = amount;
        }
        resetReady();
    }

    public double getMoney(Player player) {
        if (player.equals(requester)) {
            return requesterMoney;
        } else if (player.equals(target)) {
            return targetMoney;
        }
        return 0;
    }
}
