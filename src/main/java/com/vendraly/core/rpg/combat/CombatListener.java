package com.vendraly.core.rpg.combat;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.combat.AttackDirection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final Main plugin;
    private final DirectionalAttackManager attackManager;

    public CombatListener(Main plugin, DirectionalAttackManager attackManager) {
        this.plugin = plugin;
        this.attackManager = attackManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        if (!(damager instanceof Player attacker) || !(target instanceof Player defender)) {
            return; // solo PvP jugadores por ahora
        }

        // Cancelamos el daño vanilla
        event.setCancelled(true);

        // Dirección del ataque según el atacante
        AttackDirection attackDir = attackManager.getDirectionFromView(attacker);

        // Dirección de la defensa según el defensor
        AttackDirection defenseDir = attackManager.getDirectionFromView(defender);

        // Estado del defensor
        boolean isBlocking = defender.isBlocking();
        boolean isPerfectBlock = checkPerfectBlock(defender); // timing verde
        boolean isDodging = isDodging(defender);              // Shift + stamina
        boolean dodgeAvailable = dodgeCooldownReady(defender);

        // Validar si el defensor realmente mira hacia el atacante
        boolean facingAttacker = isFacing(defender, attacker, 60); // 60° de tolerancia
        if (!facingAttacker) {
            // no estaba mirando → no puede bloquear
            isBlocking = false;
            isPerfectBlock = false;
        }

        // Obtenemos los RPGStats
        RPGStats atkStats = plugin.getStatManager().getStats(attacker.getUniqueId());
        RPGStats defStats = plugin.getStatManager().getStats(defender.getUniqueId());

        // Llamamos al motor de daño
        DamageEngine.DamageResult result = DamageEngine.calculateDamage(
                atkStats, defStats,
                attackDir, defenseDir,
                isBlocking, isPerfectBlock,
                isDodging, dodgeAvailable
        );

        // Aplicamos daño real usando Bukkit API
        if (result.getDamage() > 0) {
            defender.damage(result.getDamage(), attacker);
        }

        // Mensajes debug (luego se cambian a ActionBar/partículas/sonidos)
        attacker.sendMessage("§cAtacaste con " + attackDir + " → " + result.getMessage() + " (" + result.getDamage() + ")");
        defender.sendMessage("§aDefendiste contra " + attackDir + " → " + result.getMessage() + " (" + result.getDamage() + ")");
    }

    /**
     * Determina si el defensor mira hacia el atacante.
     */
    private boolean isFacing(Player defender, Player attacker, double toleranceDegrees) {
        org.bukkit.util.Vector toAttacker = attacker.getLocation().toVector()
                .subtract(defender.getLocation().toVector()).normalize();
        org.bukkit.util.Vector facing = defender.getLocation().getDirection().normalize();
        double angle = Math.toDegrees(facing.angle(toAttacker));
        return angle < toleranceDegrees;
    }

    /**
     * Comprueba si fue un bloqueo perfecto (ejemplo con timing verde).
     * Aquí deberías leer un timestamp de DirectionalAttackManager.
     */
    private boolean checkPerfectBlock(Player defender) {
        Long lastGreen = attackManager.getLastGreenTime(defender); // necesitas exponer esto en el manager
        if (lastGreen == null) return false;
        long now = System.currentTimeMillis();
        return (now - lastGreen) <= 300; // ventana de 300ms
    }

    /**
     * Comprueba si el jugador está esquivando (Shift + stamina).
     */
    private boolean isDodging(Player player) {
        RPGStats stats = plugin.getStatManager().getStats(player.getUniqueId());
        if (player.isSneaking() && dodgeCooldownReady(player) && stats.getCurrentStamina() >= 5) {
            stats.consumeStamina(5);
            return true;
        }
        return false;
    }

    /**
     * Comprueba si el jugador tiene el cooldown de esquiva listo.
     * Aquí puedes implementar un Map<UUID, Long> para registrar cooldowns.
     */
    private boolean dodgeCooldownReady(Player player) {
        // TODO: implementar cooldown real con un Map en este listener o en un manager
        return true;
    }
}
