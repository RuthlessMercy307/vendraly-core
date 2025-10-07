package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.combat.DirectionalAttackManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Escucha cambios en la rotación de cámara de los jugadores
 * y alimenta esos datos al DirectionalAttackManager.
 */
public class CameraChangeListener implements Listener {

    private final DirectionalAttackManager manager;
    private static final float ROTATION_THRESHOLD = 0.1f; // tolerancia mínima

    public CameraChangeListener(Main plugin) {
        this.manager = plugin.getDirectionalAttackManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        float yawDiff = Math.abs(from.getYaw() - to.getYaw());
        float pitchDiff = Math.abs(from.getPitch() - to.getPitch());

        // Solo procesamos si realmente hubo cambio visible en cámara
        if (yawDiff < ROTATION_THRESHOLD && pitchDiff < ROTATION_THRESHOLD) return;

        Player player = event.getPlayer();
        manager.recordCameraMovement(player, to);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.createIndicator(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removeIndicator(event.getPlayer());
    }
}
