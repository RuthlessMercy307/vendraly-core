package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

/**
 * Scheduler de regeneración para modo RPG-ONLY:
 * - La vida/regeneración se maneja SOLO en RPGStats (nada de setHealth vanilla).
 * - Estamina, cooldown de cansancio y BossBar de estamina se mantienen.
 */
public class RegenerationScheduler {

    private final Main plugin;
    private final StatManager statManager;
    private final AttributeApplier attributeApplier; // lo mantenemos por coherencia (equip/bonos)
    private final StaminaBossBarManager bossBarManager;
    private final Map<UUID, Long> staminaCooldowns;
    private BukkitTask regenTask;

    // Config por defecto (puedes moverlos a config.yml si algún día te interesa)
    private static final long REGEN_TICK_RATE = 10L; // cada 0.5s
    private static final int STAMINA_COOLDOWN_SECONDS = 3;
    private static final long STAMINA_COOLDOWN_MS = STAMINA_COOLDOWN_SECONDS * 1000L;
    private static final double STAMINA_COST_PER_SECOND = 4.0;

    public RegenerationScheduler(Main plugin,
                                 StatManager statManager,
                                 AttributeApplier attributeApplier,
                                 StaminaBossBarManager bossBarManager,
                                 Map<UUID, Long> staminaCooldowns) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.attributeApplier = attributeApplier;
        this.bossBarManager = bossBarManager;
        this.staminaCooldowns = staminaCooldowns;
    }

    /** Inicia la tarea periódica (RPG-ONLY) */
    public void startRegenScheduler() {
        if (regenTask != null && !regenTask.isCancelled()) return;

        plugin.getLogger().info("[StatManager] Iniciando tarea de Regeneración (RPG-ONLY Health/Stamina)...");

        regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Requiere estar autenticado y con stats cargados
                    if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) continue;

                    RPGStats stats = statManager.getStats(player.getUniqueId());
                    if (stats == null) continue;

                    UUID uuid = player.getUniqueId();
                    long now = System.currentTimeMillis();
                    boolean isOnCooldown = false;

                    // Factor de ticks: convierte valores/segundo a valores/ejecución
                    double tickFactor = (20.0 / (double) REGEN_TICK_RATE);
                    double staminaRegenPerTick = stats.getStaminaRegenPerSecond() / tickFactor;
                    double staminaCostPerTick = STAMINA_COST_PER_SECOND / tickFactor;
                    boolean isSprinting = player.isSprinting();

                    // ---------- COOLDOWN DE ESTAMINA ----------
                    if (staminaCooldowns.containsKey(uuid)) {
                        long cooldownEnd = staminaCooldowns.get(uuid);
                        if (cooldownEnd > now) {
                            isOnCooldown = true;
                            applySlownessDebuff(player);

                            if (isSprinting) {
                                player.setSprinting(false);
                                isSprinting = false;
                            }
                        } else {
                            // terminó cooldown
                            staminaCooldowns.remove(uuid);
                            player.removePotionEffect(PotionEffectType.SLOWNESS);
                        }
                    }

                    // ---------- ESTAMINA (gasto/regen) ----------
                    if (isSprinting) {
                        double newStamina = stats.getCurrentStamina() - staminaCostPerTick;
                        if (newStamina <= 0.0) {
                            stats.setCurrentStamina(0.0);
                            player.setSprinting(false);

                            if (!isOnCooldown) {
                                staminaCooldowns.put(uuid, now + STAMINA_COOLDOWN_MS);
                                player.sendActionBar(Component.text("¡Estamina agotada!").color(NamedTextColor.YELLOW));
                                applySlownessDebuff(player);
                            }
                        } else {
                            stats.setCurrentStamina(newStamina);
                        }
                    } else if (!isOnCooldown) {
                        if (staminaRegenPerTick > 0.0 && stats.getCurrentStamina() < stats.getMaxStamina()) {
                            stats.setCurrentStamina(stats.getCurrentStamina() + staminaRegenPerTick);
                        }
                    }

                    // Clamp
                    stats.setCurrentStamina(Math.max(0.0, Math.min(stats.getCurrentStamina(), stats.getMaxStamina())));

                    // BossBar de estamina
                    bossBarManager.updateStaminaBossBar(player, stats);

                    // ---------- VIDA (RPG-ONLY) ----------
                    // Usamos SOLO RPGStats.current/max health (nada de vanilla)
                    double rpgMaxHealth = stats.getMaxHealth();
                    double healthRegenPerTick = stats.getHealthRegenPerSecond() / tickFactor;

                    // Regenera vida solo si no está corriendo y no está en cooldown
                    if (healthRegenPerTick > 0 && stats.getCurrentHealth() < rpgMaxHealth && !isSprinting && !isOnCooldown) {
                        double newHp = Math.min(stats.getCurrentHealth() + healthRegenPerTick, rpgMaxHealth);
                        stats.setCurrentHealth(newHp);

                        // Scoreboard (si quieres ver el número subir)
                        if (plugin.getScoreboardManager() != null) {
                            plugin.getScoreboardManager().notifyHealthChange(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, REGEN_TICK_RATE, REGEN_TICK_RATE);
    }

    /** Aplica lentitud mientras dura el cooldown de estamina. */
    private void applySlownessDebuff(Player player) {
        long durationTicks = STAMINA_COOLDOWN_SECONDS * 20L + REGEN_TICK_RATE;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                (int) durationTicks,
                0,
                false, false, false
        ));
    }

    /** Detiene la tarea. */
    public void stopRegenScheduler() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
    }

    public void ensurePlayerTask(UUID uuid) {
        if (regenTask == null || regenTask.isCancelled()) {
            startRegenScheduler();
        }
    }

}
