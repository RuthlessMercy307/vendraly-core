package com.vendraly.core.rpg.listener;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.UUID;

/**
 * Listener robusto para aplicar y remover las bonificaciones de atributos
 * de ítems RPG (armas/armaduras custom) al ser equipados, desequipados o movidos.
 *
 * Todo el cálculo de stats se delega a AttributeApplier.recalculateEquippedBonuses()
 * para evitar duplicación de lógica.
 */
public class EquipmentListener implements Listener {

    private final Main plugin;
    private final StatManager statManager;

    public EquipmentListener(Main plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        // Solo interesa inventario de jugador
        if (event.getClickedInventory().getType() != InventoryType.PLAYER) return;

        boolean relevantAction =
                isEquipmentSlot(event.getSlot()) || // clic directo en armadura/mano
                        event.isShiftClick();               // shift move

        if (relevantAction) {
            scheduleRecalc(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Si alguno de los slots arrastrados es de equipamiento
        for (int slot : event.getRawSlots()) {
            if (isEquipmentSlot(slot)) {
                scheduleRecalc(player);
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Al cerrar inventario, refrescamos todo (por seguridad ante casos no captados)
        scheduleRecalc(player);
    }

    /**
     * Marca un recalculo de stats con delay de 1 tick,
     * para que el inventario esté actualizado tras el evento.
     */
    private void scheduleRecalc(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            RPGStats stats = statManager.getStats(uuid);
            if (stats == null) return;

            // Recalcular bonus de equipo
            statManager.getAttributeApplier().recalculateEquippedBonuses(player);

            // Aplicar atributos vanilla + bossbars personalizados
            statManager.updatePlayerVisuals(player);

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updateScoreboard(player);
            }
        }, 1L);
    }

    /**
     * Comprueba si un slot corresponde a equipamiento (armadura o off-hand).
     * Slots: 36-39 (armadura), 40 (off-hand).
     */
    private boolean isEquipmentSlot(int slot) {
        return (slot >= 36 && slot <= 40);
    }
}
