package com.vendraly.core.rpg.combat;

import com.vendraly.core.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Indicador direccional de ataque tipo Kingdom Come Deliverance.
 * Implementado con ArmorStand invisible + texto flotante (holograma).
 * Estable, persistente y visible para otros jugadores sin depender de la cámara.
 */
public class DirectionalAttackManager {

    private final Main plugin;

    private final Map<UUID, ArmorStand> holograms = new HashMap<>();
    private final Map<UUID, Location> lastCameraLocation = new HashMap<>();
    private final Map<UUID, AttackDirection> activeDirection = new HashMap<>();
    private final Map<UUID, AttackDirection> lastRenderedDirection = new HashMap<>();
    private final Map<UUID, Boolean> isAttacking = new HashMap<>();

    // ⬇️ NUEVO: mapa para guardar el último “timing verde” (perfect block)
    private final Map<UUID, Long> lastGreenTimes = new HashMap<>();

    private static final double MIN_YAW_CHANGE = 5.0;
    private static final double MIN_PITCH_CHANGE = 3.0;
    private static final double INDICATOR_HEIGHT = 2.3;
    private static final long ATTACK_DELAY_TICKS = 10L; // 0.5 segundos

    public DirectionalAttackManager(Main plugin) {
        this.plugin = plugin;
    }

    // ===========================================================
    // == INICIO DE ATAQUE ==
    // ===========================================================
    public boolean startAttackDelay(Player player) {
        UUID uuid = player.getUniqueId();
        if (isAttacking.getOrDefault(uuid, false)) return false;

        isAttacking.put(uuid, true);
        final AttackDirection finalDirection = getLastNonNeutralDirection(player);

        // Verde (inicio de wind-up)
        setSymbolColor(player, finalDirection, NamedTextColor.GREEN);
        // Guardamos el “timing verde”
        markGreenTiming(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Rojo (ataque)
                setSymbolColor(player, finalDirection, NamedTextColor.RED);
                executeDirectionalAttack(player, finalDirection);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isAttacking.put(uuid, false);
                        // Dorado (standby de nuevo)
                        setSymbolColor(player, finalDirection, NamedTextColor.GOLD);
                    }
                }.runTaskLater(plugin, ATTACK_DELAY_TICKS);
            }
        }.runTaskLater(plugin, ATTACK_DELAY_TICKS);

        return true;
    }

    /**
     * Ejecuta el ataque direccional usando el DamageEngine.
     */
    private void executeDirectionalAttack(Player player, AttackDirection direction) {
        try {
            DamageEngine.applyDirectionalAttack(player, direction);

            player.getWorld().playSound(player.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

            player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK,
                    player.getLocation().add(0, 1, 0), 3);

        } catch (Throwable t) {
            player.sendMessage("§c[Error] Falló el ataque direccional: " + t.getMessage());
            t.printStackTrace();
        }
    }

    // ===========================================================
    // == COLORES ==
    // ===========================================================
    private void setSymbolColor(Player player, AttackDirection direction, NamedTextColor color) {
        ArmorStand stand = holograms.get(player.getUniqueId());
        if (stand == null || stand.isDead()) return;
        String symbol = directionToSymbol(direction);
        stand.customName(Component.text(symbol, color));
    }

    // ===========================================================
    // == CREACIÓN DEL HOLOGRAMA ==
    // ===========================================================
    public void createIndicator(Player player) {
        UUID uuid = player.getUniqueId();
        removeIndicator(player);

        Location spawnLoc = player.getLocation().add(0, INDICATOR_HEIGHT, 0);
        ArmorStand stand = player.getWorld().spawn(spawnLoc, ArmorStand.class, as -> {
            as.setGravity(false);
            as.setVisible(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.customName(Component.text("➡", NamedTextColor.GOLD));
        });

        holograms.put(uuid, stand);
        lastCameraLocation.put(uuid, player.getLocation().clone());
        activeDirection.put(uuid, AttackDirection.RIGHT); // valor inicial visible
        lastRenderedDirection.put(uuid, AttackDirection.RIGHT);
        isAttacking.put(uuid, false);

        startHologramUpdater(player, stand);
    }

    public void removeIndicator(Player player) {
        UUID uuid = player.getUniqueId();
        ArmorStand stand = holograms.remove(uuid);
        if (stand != null && !stand.isDead()) stand.remove();

        lastCameraLocation.remove(uuid);
        activeDirection.remove(uuid);
        lastRenderedDirection.remove(uuid);
        isAttacking.remove(uuid);
        lastGreenTimes.remove(uuid);
    }

    private void startHologramUpdater(Player player, ArmorStand stand) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                if (!player.isOnline() || !holograms.containsKey(uuid)) {
                    removeIndicator(player);
                    cancel();
                    return;
                }

                if (stand.isDead()) {
                    cancel();
                    Bukkit.getScheduler().runTask(plugin, () -> createIndicator(player));
                    return;
                }

                stand.teleport(player.getLocation().add(0, INDICATOR_HEIGHT, 0));

                if (!isAttacking.getOrDefault(uuid, false)) {
                    updateHologram(player, stand);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ===========================================================
    // == MOVIMIENTO DE CÁMARA ==
    // ===========================================================
    public void recordCameraMovement(Player player, Location newLocation) {
        UUID uuid = player.getUniqueId();
        Location old = lastCameraLocation.get(uuid);
        if (old == null) {
            lastCameraLocation.put(uuid, newLocation.clone());
            return;
        }

        double yawChange = newLocation.getYaw() - old.getYaw();
        double pitchChange = newLocation.getPitch() - old.getPitch();

        if (yawChange > 180) yawChange -= 360;
        if (yawChange < -180) yawChange += 360;

        AttackDirection newDir = determineDirectionByMovement(yawChange, pitchChange);
        AttackDirection lastDir = activeDirection.getOrDefault(uuid, AttackDirection.RIGHT);

        if (newDir != AttackDirection.NEUTRAL && newDir != lastDir) {
            activeDirection.put(uuid, newDir);
        }

        lastCameraLocation.put(uuid, newLocation.clone());
    }

    private AttackDirection determineDirectionByMovement(double yawChange, double pitchChange) {
        boolean horizontal = Math.abs(yawChange) > MIN_YAW_CHANGE;
        boolean vertical = Math.abs(pitchChange) > MIN_PITCH_CHANGE;

        if (vertical && !horizontal) {
            if (pitchChange < 0) return AttackDirection.UP;
        } else if (horizontal && !vertical) {
            return yawChange < 0 ? AttackDirection.LEFT : AttackDirection.RIGHT;
        } else if (horizontal && vertical) {
            if (pitchChange > 0)
                return yawChange < 0 ? AttackDirection.DIAGONAL_LEFT_DOWN : AttackDirection.DIAGONAL_RIGHT_DOWN;
        }
        return AttackDirection.NEUTRAL;
    }

    // ===========================================================
    // == ACTUALIZACIÓN DEL TEXTO ==
    // ===========================================================
    private void updateHologram(Player player, ArmorStand stand) {
        UUID uuid = player.getUniqueId();
        AttackDirection dir = activeDirection.getOrDefault(uuid, lastRenderedDirection.getOrDefault(uuid, AttackDirection.RIGHT));

        if (dir == AttackDirection.NEUTRAL)
            dir = lastRenderedDirection.getOrDefault(uuid, AttackDirection.RIGHT);

        if (dir == lastRenderedDirection.get(uuid)) return;
        lastRenderedDirection.put(uuid, dir);

        stand.customName(Component.text(directionToSymbol(dir), NamedTextColor.GOLD));
    }

    private String directionToSymbol(AttackDirection dir) {
        return switch (dir) {
            case UP -> "⬆";
            case LEFT -> "⬅";
            case RIGHT -> "➡";
            case DIAGONAL_LEFT_DOWN -> "↙";
            case DIAGONAL_RIGHT_DOWN -> "↘";
            default -> "➡";
        };
    }

    private AttackDirection getLastNonNeutralDirection(Player player) {
        AttackDirection current = activeDirection.getOrDefault(player.getUniqueId(), AttackDirection.RIGHT);
        if (current == AttackDirection.NEUTRAL)
            return lastRenderedDirection.getOrDefault(player.getUniqueId(), AttackDirection.RIGHT);
        return current;
    }

    public AttackDirection getActiveDirection(Player player) {
        return getLastNonNeutralDirection(player);
    }

    // ===========================================================
    // == TIMING VERDE ==
    // ===========================================================
    public void markGreenTiming(Player player) {
        lastGreenTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public Long getLastGreenTime(Player player) {
        return lastGreenTimes.get(player.getUniqueId());
    }

    // ===========================================================
    // == STOP ==
    // ===========================================================
    public void stop() {
        holograms.values().forEach(ArmorStand::remove);
        holograms.clear();
        lastCameraLocation.clear();
        activeDirection.clear();
        lastRenderedDirection.clear();
        isAttacking.clear();
        lastGreenTimes.clear();
    }
}
