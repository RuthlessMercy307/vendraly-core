package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.stats.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class StatListener implements Listener {

    private final Main plugin;
    private final StatManager statManager;

    // --- Costes de estamina ---
    private static final double STAMINA_COST_PER_JUMP = 5.0;
    private static final double STAMINA_MINIMUM_FOR_SPRINT = 5.0;
    private static final double STAMINA_COST_PER_ATTACK = 2.0;
    private static final double STAMINA_COST_PER_BLOCK_BREAK = 1.0;

    // --- Reducción de daño fija por punto de Defensa ---
    public static final double FLAT_DAMAGE_REDUCTION_PER_DEFENSE_POINT = 0.1;

    // Título esperado del menú (debe coincidir con StatManager.openStatMenu)
    private static final Component STATS_MENU_TITLE =
            Component.text("Atributos RPG").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD);

    public StatListener(Main plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    private PlayerData getPlayerData(UUID uuid) {
        return plugin.getAuthManager().getPlayerData(uuid);
    }

    // =========================================================
    //   UTILIDADES DE ESTAMINA
    // =========================================================
    private boolean tryConsumeStamina(Player player, RPGStats stats, double cost, String actionName) {
        if (cost <= 0.0) return true;

        UUID uuid = player.getUniqueId();

        // Cooldown de cansancio
        if (statManager.getStaminaCooldowns().containsKey(uuid)) {
            player.sendActionBar(Component.text("Cansancio: espera a recuperarte.")
                    .color(NamedTextColor.YELLOW));
            return false;
        }

        // Consumo real
        if (!stats.consumeStamina(cost)) {
            player.sendActionBar(Component.text("¡Sin estamina para " + actionName + "!")
                    .color(NamedTextColor.RED));
            statManager.updateStaminaBossBar(player, stats);
            return false;
        }

        statManager.updateStaminaBossBar(player, stats);
        return true;
    }

    // =========================================================
    //   EVENTOS DE ESTAMINA
    // =========================================================
    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (!plugin.getAuthManager().isAuthenticated(uuid)) return;

        // Solo al iniciar sprint
        if (event.isSprinting()) {
            final RPGStats stats = statManager.getStats(uuid);
            if (stats == null) return;

            if (statManager.getStaminaCooldowns().containsKey(uuid)) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("No puedes correr: cansancio.")
                        .color(NamedTextColor.YELLOW));
                return;
            }

            if (stats.getCurrentStamina() < STAMINA_MINIMUM_FOR_SPRINT) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("Necesitas " + STAMINA_MINIMUM_FOR_SPRINT + " de estamina para correr.")
                        .color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (!plugin.getAuthManager().isAuthenticated(uuid)) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to == null) return;

        // Solo si sube (intento de salto)
        if (to.getY() <= from.getY()) return;

        // Debe tener velocidad vertical positiva, no volar, no estar en agua
        if (player.getVelocity().getY() <= 0.0 || player.isFlying() || player.isInWater()) return;

        // Debe estar sobre un bloque sólido al iniciar el salto
        final Block blockUnder = from.clone().subtract(0, 0.001, 0).getBlock();
        if (!blockUnder.getType().isSolid()) return;

        final RPGStats stats = statManager.getStats(uuid);
        if (stats == null) return;

        if (!tryConsumeStamina(player, stats, STAMINA_COST_PER_JUMP, "saltar")) {
            // Bloquear salto
            event.setCancelled(true);
            player.setVelocity(player.getVelocity().setY(-0.1));
        }
    }

    // =========================================================
    //   DAÑO / DEFENSA
    // =========================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;
        if (event.getDamage() <= 0.0) return;

        final RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        final double flatReduction = stats.getStatDefense() * FLAT_DAMAGE_REDUCTION_PER_DEFENSE_POINT;
        if (flatReduction <= 0.0) return;

        final double before = event.getDamage();
        final double after = Math.max(0.0, before - flatReduction);
        event.setDamage(after);

        if (after < before) {
            player.sendActionBar(
                    Component.text("Defensa -")
                            .append(Component.text(String.format("%.2f", flatReduction)))
                            .append(Component.text(" daño fijo"))
                            .color(NamedTextColor.DARK_AQUA)
            );
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        final RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        // Si no puede pagar la estamina, cancelamos el golpe vanilla
        if (!tryConsumeStamina(player, stats, STAMINA_COST_PER_ATTACK, "atacar")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        final RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        if (!tryConsumeStamina(player, stats, STAMINA_COST_PER_BLOCK_BREAK, "trabajar")) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    //   EXP RPG (bloquea vanilla y redirige a RPG)
    // =========================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (!plugin.getAuthManager().isAuthenticated(uuid)) return;
        if (event.getAmount() <= 0) return;

        final PlayerData data = getPlayerData(uuid);
        if (data == null) return;

        final RPGStats stats = data.getRpgStats();
        final boolean levelUp = stats.addExpTotal(event.getAmount());

        // Bloquear vanilla
        event.setAmount(0);
        player.setExp(0f);

        // Mostrar progreso RPG en barra vanilla
        updateVanillaLevelDisplay(player, stats);

        if (levelUp) handleLevelUp(player, stats);
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        final Player player = event.getPlayer();
        final PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null) return;

        final RPGStats stats = data.getRpgStats();

        // Si Minecraft intentó cambiar el nivel visual, lo sincronizamos con RPG
        if (event.getNewLevel() != stats.getRpgLevel()) {
            updateVanillaLevelDisplay(player, stats);
        }
    }

    private void updateVanillaLevelDisplay(Player player, RPGStats stats) {
        player.setLevel(stats.getRpgLevel());
        player.setExp(stats.getExpToNextLevel()); // 0.0–1.0
    }

    private void handleLevelUp(Player player, RPGStats stats) {
        final int newLevel = stats.getRpgLevel();
        final int points = 3; // puntos por nivel

        stats.addUnspentPoints(points);

        player.sendMessage(Component.text("¡Has subido a nivel ")
                .append(Component.text(newLevel).color(NamedTextColor.AQUA))
                .append(Component.text("! +" + points + " puntos disponibles.").color(NamedTextColor.YELLOW)));

        statManager.applyPlayerAttributes(player, stats);
        statManager.startRegenScheduler();

        final PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) plugin.getUserDataManager().savePlayerData(data);

        // Notificar (si existe) al scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().notifyHealthChange(player);
        }
    }

    // =========================================================
    //   ATRIBUTOS MENÚ (con StatType)
    // =========================================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Verificamos título del menú con PlainText (evita problemas de formato)
        final String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        final String expectedTitle = PlainTextComponentSerializer.plainText().serialize(STATS_MENU_TITLE);
        if (!title.equals(expectedTitle)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() == player.getInventory()) return;

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        final ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        final String rawName = PlainTextComponentSerializer.plainText().serialize(meta.displayName()).trim();

        final PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null) return;

        final RPGStats stats = data.getRpgStats();
        if (stats.getUnspentPoints() < 1) {
            player.sendMessage(Component.text("¡No tienes puntos disponibles!").color(NamedTextColor.RED));
            return;
        }

        // El botón suele tener formato "Mejorar <Nombre>" -> extraemos nombre
        final String displayName = rawName.replace("Mejorar ", "").trim();

        final StatType type = StatType.fromDisplayName(displayName);
        if (type == null) {
            player.sendMessage(Component.text("Atributo desconocido: ").append(Component.text(displayName).color(NamedTextColor.RED)));
            return;
        }

        if (applyPointToStat(stats, type)) {
            // Aplicar y guardar
            statManager.applyPlayerAttributes(player, stats);
            plugin.getUserDataManager().savePlayerData(data);

            // Reabrir el menú para refrescar
            statManager.openStatMenu(player);

            // Feedback
            player.sendMessage(Component.text("Punto asignado a ")
                    .append(Component.text(type.getDisplayName()).color(NamedTextColor.GOLD))
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No se pudo asignar el punto a ")
                    .append(Component.text(type.getDisplayName()).color(NamedTextColor.RED)));
        }
    }

    private boolean applyPointToStat(RPGStats stats, StatType type) {
        switch (type) {
            case HEALTH_MAX:        return stats.increaseStatHealth();
            case STRENGTH:          return stats.increaseStatStrength();
            case DEFENSE:           return stats.increaseStatDefense();
            case MOVEMENT_SPEED:    return stats.increaseStatMovementSpeed();
            case MINING_SPEED:      return stats.increaseStatMiningSpeed();
            case WOODCUTTING_SPEED: return stats.increaseStatWoodcuttingSpeed();
            case HEALTH_REGEN:      return stats.increaseStatHealthRegen();
            case STAMINA_MAX:       return stats.increaseStatStaminaMax();
            case STAMINA_REGEN:     return stats.increaseStatStaminaRegen();
            default:                return false;
        }
    }

    // =========================================================
    //   LOGOUT CLEANUP
    // =========================================================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        // Limpieza visual
        statManager.removeStaminaBossBar(player);

        // Quitar efectos de estado RPG
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        // Limpiar cooldowns
        statManager.getStaminaCooldowns().remove(player.getUniqueId());

        // Guardar
        final PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) plugin.getUserDataManager().savePlayerData(data);
    }
}
