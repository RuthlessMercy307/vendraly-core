package com.vendraly.core.rpg;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento custom que se dispara cuando un jugador sube de nivel RPG.
 *
 * - Contiene el jugador, nivel anterior y nuevo nivel.
 * - Puede ser cancelado: si se cancela, la subida de nivel no se aplica.
 */
public class PlayerLevelUpEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    private boolean cancelled;

    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    /* ----------------------
       Soporte Cancellable
       ---------------------- */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /* ----------------------
       Bukkit Handlers
       ---------------------- */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
