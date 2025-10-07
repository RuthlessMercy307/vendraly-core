package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.combat.DirectionalAttackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener encargado de capturar ataques y defensas direccionales de los jugadores.
 */
public class DirectionalAttackListener implements Listener {

    private final DirectionalAttackManager manager;
    private final Main plugin;

    // Tiempo de la última defensa de cada jugador (Concurrent para hilos asíncronos)
    private final Map<UUID, Long> defendingPlayers = new ConcurrentHashMap<>();

    // Margen de tiempo para considerar defensa válida
    private static final long DEFENSE_WINDOW_MS = 1000L;

    public DirectionalAttackListener(Main plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDirectionalAttackManager();
    }

    // =========================================================
    //   ATAQUES (Click izquierdo)
    // =========================================================
    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttack(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        boolean attackStarted = manager.startAttackDelay(player);

        if (attackStarted) {
            // Cancelamos el evento vanilla para evitar doble interacción
            event.setCancelled(true);
            player.sendActionBar(Component.text("Ataque direccional iniciado.").color(NamedTextColor.RED));
        }
    }

    // =========================================================
    //   DEFENSA (Click derecho con espada)
    // =========================================================
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDefend(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;
        if (!item.getType().name().contains("SWORD")) return;

        defendingPlayers.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendActionBar(Component.text("Defendiendo...").color(NamedTextColor.AQUA));
    }

    // =========================================================
    //   COLISIÓN DE ATAQUE - LLAMADA EXTERNA
    // =========================================================
    /**
     * Llamar desde DamageEngine o DirectionalAttackManager al detectar colisión.
     */
    public void onAttackCollision(Player attacker, Player defender) {
        UUID defUuid = defender.getUniqueId();
        long lastDefend = defendingPlayers.getOrDefault(defUuid, 0L);

        long elapsed = System.currentTimeMillis() - lastDefend;
        boolean defended = elapsed < DEFENSE_WINDOW_MS;

        if (defended) {
            defender.sendMessage(Component.text("¡Defensa perfecta!").color(NamedTextColor.GREEN));
            attacker.sendMessage(Component.text("¡Tu ataque fue bloqueado y pierdes 20 de estamina!").color(NamedTextColor.RED));

            // Hook opcional: integración con stamina
            // plugin.getStatManager().consumeStamina(attacker, 20);

        } else {
            defender.sendMessage(Component.text("Defensa tardía. Recibes daño parcial.").color(NamedTextColor.YELLOW));
        }
    }

    // =========================================================
    //   UTILIDAD
    // =========================================================
    /**
     * Devuelve si un jugador está actualmente en "ventana de defensa".
     */
    public boolean isDefending(UUID uuid) {
        long lastDefend = defendingPlayers.getOrDefault(uuid, 0L);
        return (System.currentTimeMillis() - lastDefend) < DEFENSE_WINDOW_MS;
    }
}
