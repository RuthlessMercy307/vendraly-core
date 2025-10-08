package com.vendraly.core.rpg.combat;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Indicador direccional estilo KCD:
 * - Dirección a partir del vector de cámara
 * - Cooldown anti-macro
 * - Holograma con [Lv. X] y flecha
 * - Partículas rojas (ataque) / azules (defensa)
 */
public class DirectionalAttackManager {

    private final Main plugin;

    private final Map<UUID, ArmorStand> holograms = new HashMap<>();
    private final Map<UUID, AttackDirection> lastRenderedDirection = new HashMap<>();
    private final Map<UUID, Boolean> isAttacking = new HashMap<>();
    private final Map<UUID, Long> lastGreenTimes = new HashMap<>();
    private final Map<UUID, Long> lastAttackMs = new HashMap<>();

    private static final double INDICATOR_HEIGHT = 2.3;
    private static final long   ATTACK_DELAY_TICKS = 10L;     // wind-up 0.5s
    private static final long   ATTACK_COOLDOWN_MS = 600L;    // anti-macro real

    public DirectionalAttackManager(Main plugin) {
        this.plugin = plugin;
    }

    // ===========================================================
    // == INICIO DE ATAQUE (con cooldown real) ==
    // ===========================================================
    public boolean startAttackDelay(Player player) {
        final UUID uuid = player.getUniqueId();

        // anti-macro
        long now = System.currentTimeMillis();
        if (now - lastAttackMs.getOrDefault(uuid, 0L) < ATTACK_COOLDOWN_MS) {
            long left = ATTACK_COOLDOWN_MS - (now - lastAttackMs.get(uuid));
            player.sendActionBar(Component.text("⏳ Enfriamiento: " + left + "ms", NamedTextColor.GRAY));
            return false;
        }

        if (isAttacking.getOrDefault(uuid, false)) return false;

        isAttacking.put(uuid, true);
        final AttackDirection finalDirection = getDirectionFromView(player);

        // Verde (inicio de wind-up)
        setName(player, finalDirection, NamedTextColor.GREEN);
        markGreenTiming(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Rojo (ataque) y sello cooldown aquí
                lastAttackMs.put(uuid, System.currentTimeMillis());
                setName(player, finalDirection, NamedTextColor.RED);
                executeDirectionalAttack(player, finalDirection);

                // Volver a dorado (idle) tras el mismo delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isAttacking.put(uuid, false);
                        setName(player, getDirectionFromView(player), NamedTextColor.GOLD);
                    }
                }.runTaskLater(plugin, ATTACK_DELAY_TICKS);
            }
        }.runTaskLater(plugin, ATTACK_DELAY_TICKS);

        return true;
    }

    // ===========================================================
    // == ATAQUE ==
    // ===========================================================
    private void executeDirectionalAttack(Player player, AttackDirection direction) {
        try {
            DamageEngine.applyDirectionalAttack(player, direction);

            player.getWorld().playSound(
                    player.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f
            );

            spawnDirectionalParticles(player, direction, true);
            player.sendActionBar(Component.text("⚔ Ataque: " + direction.name(), NamedTextColor.RED));
            plugin.getLogger().info(player.getName() + " realizó ataque " + direction);

        } catch (Throwable t) {
            player.sendMessage("§c[Error] Falló el ataque direccional: " + t.getMessage());
            t.printStackTrace();
        }
    }

    // ===========================================================
    // == PARTÍCULAS (ataque rojo / defensa azul) ==
    // ===========================================================
    private void spawnDirectionalParticles(Player player, AttackDirection dir, boolean isAttack) {
        Location base = player.getLocation().add(0, 1.2, 0); // pecho
        Location loc = base.clone();

        // Usa yaw para desplazar el efecto alrededor del jugador
        float yaw = player.getLocation().getYaw();
        double rad = Math.toRadians(yaw);

        switch (dir) {
            case UP -> loc.add(-Math.sin(rad), 0, Math.cos(rad)); // frente
            case LEFT -> loc.add(-Math.cos(rad), 0, -Math.sin(rad)); // izquierda
            case RIGHT -> loc.add(Math.cos(rad), 0, Math.sin(rad));  // derecha
            case DIAGONAL_LEFT_DOWN -> loc.add(-Math.cos(rad) * 0.7 - Math.sin(rad) * 0.7, 0,
                    -Math.sin(rad) * 0.7 + Math.cos(rad) * 0.7);
            case DIAGONAL_RIGHT_DOWN -> loc.add(Math.cos(rad) * 0.7 - Math.sin(rad) * -0.7, 0,
                    Math.sin(rad) * 0.7 + Math.cos(rad) * 0.7);
            case NEUTRAL -> { /* centro */ }
        }

        if (isAttack) {
            player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, loc, 8, 0.2, 0.2, 0.2, 0.05);
        } else {
            player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, loc, 15, 0.4, 0.4, 0.4, 0.1);
        }
    }

    public void showDefenseParticles(Player player, AttackDirection dir) {
        spawnDirectionalParticles(player, dir, false);
        player.sendActionBar(Component.text("🛡 Defensa " + dir.name(), NamedTextColor.AQUA));
        setName(player, dir, NamedTextColor.AQUA);
    }

    // ===========================================================
    // == HOLOGRAMA ==
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
            as.customName(Component.text("...", NamedTextColor.GOLD));
        });

        holograms.put(uuid, stand);
        isAttacking.put(uuid, false);

        // updater por-tick
        startHologramUpdater(player, stand);
    }

    public void removeIndicator(Player player) {
        UUID uuid = player.getUniqueId();
        ArmorStand stand = holograms.remove(uuid);
        if (stand != null && !stand.isDead()) stand.remove();
        lastRenderedDirection.remove(uuid);
        isAttacking.remove(uuid);
        lastGreenTimes.remove(uuid);
        lastAttackMs.remove(uuid);
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

                // mantener el holograma sobre el jugador
                stand.teleport(player.getLocation().add(0, INDICATOR_HEIGHT, 0));

                // si no está en animación de ataque, actualiza dirección/level y color dorado
                if (!isAttacking.getOrDefault(uuid, false)) {
                    AttackDirection dir = getDirectionFromView(player);
                    AttackDirection last = lastRenderedDirection.get(uuid);
                    if (dir != last) {
                        setName(player, dir, NamedTextColor.GOLD);
                        lastRenderedDirection.put(uuid, dir);
                    } else {
                        // refresca solo el nivel por si sube
                        setName(player, dir, NamedTextColor.GOLD);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void setName(Player player, AttackDirection dir, NamedTextColor color) {
        ArmorStand stand = holograms.get(player.getUniqueId());
        if (stand == null || stand.isDead()) return;

        int level = 1;
        try {
            RPGStats s = plugin.getStatManager() != null
                    ? plugin.getStatManager().getStats(player.getUniqueId())
                    : null;
            if (s != null) level = Math.max(1, s.getLevel());
        } catch (Throwable ignored) {}

        String text = "[Lv. " + level + "] " + directionToSymbol(dir);
        stand.customName(Component.text(text, color));
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

    // ===========================================================
    // == DIRECCIÓN POR VECTOR DE CÁMARA ==
    // ===========================================================
    public AttackDirection getDirectionFromView(Player player) {
        Vector d = player.getLocation().getDirection().normalize();
        double x = d.getX();
        double y = d.getY();
        double z = d.getZ();

        // Umbrales
        final double Y_UP = 0.35;      // mirando suficientemente hacia arriba = UP (estocada)
        final double Y_DOWN = -0.35;   // mirando hacia abajo = diagonales
        final double H_BIAS = 0.15;    // preferencia lateral vs frontal

        if (y >= Y_UP) {
            return AttackDirection.UP;
        }
        if (y <= Y_DOWN) {
            // diagonales: decide izquierda/derecha por componente X (hacia este/oeste)
            return (x < 0) ? AttackDirection.DIAGONAL_LEFT_DOWN : AttackDirection.DIAGONAL_RIGHT_DOWN;
        }

        // en plano horizontal: decide izquierda/derecha por componente X dominante
        if (Math.abs(x) >= Math.abs(z) - H_BIAS) {
            return (x < 0) ? AttackDirection.LEFT : AttackDirection.RIGHT;
        }

        // fallback
        return AttackDirection.RIGHT;
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
        lastRenderedDirection.clear();
        isAttacking.clear();
        lastGreenTimes.clear();
        lastAttackMs.clear();
    }
}
