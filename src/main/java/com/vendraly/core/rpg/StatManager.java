package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import com.vendraly.core.database.PlayerData;
import com.vendraly.core.rpg.stats.AttributeApplier;
import com.vendraly.core.rpg.stats.LevelingManager;
import com.vendraly.core.rpg.stats.MenuBuilder;
import com.vendraly.core.rpg.stats.RegenerationScheduler;
import com.vendraly.core.rpg.stats.StaminaBossBarManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Núcleo del sistema RPG.
 * Centraliza todos los submódulos y coordina atributos, regeneración, bossbars y menús.
 */
public class StatManager implements CommandExecutor, Listener {

    private final Main plugin;

    // Truco para ocultar la barra de vida vanilla
    private static final double HIDE_VANILLA_HEALTH_VALUE = 1024.0;

    // Submódulos
    private final AttributeApplier attributeApplier;
    private final StaminaBossBarManager staminaBossBarManager;
    private final RegenerationScheduler regenerationScheduler;
    private final LevelingManager levelingManager;
    private final MenuBuilder menuBuilder;

    // Estado compartido
    private final Map<UUID, BossBar> staminaBars = new HashMap<>();
    private final Map<UUID, Long> staminaCooldowns = new HashMap<>();

    // Constantes de Skills
    public static final String BLACKSMITHING = "BLACKSMITHING";
    public static final String TAILORING = "TAILORING";
    public static final String APOTHECARY = "APOTHECARY";

    public StatManager(Main plugin) {
        this.plugin = plugin;

        // Inicializa submódulos
        this.attributeApplier = new AttributeApplier(plugin, this);
        this.staminaBossBarManager = new StaminaBossBarManager(plugin, staminaBars);
        this.levelingManager = new LevelingManager(plugin, this);
        this.regenerationScheduler = new RegenerationScheduler(plugin, this, attributeApplier, staminaBossBarManager, staminaCooldowns);
        this.menuBuilder = new MenuBuilder(plugin, this, attributeApplier, levelingManager);
    }

    /* ---------------------------
       EVENTOS CLAVE
       --------------------------- */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Forzar carga de datos RPG
        PlayerData data = plugin.getAuthManager().getPlayerData(uuid);
        if (data == null || data.getRpgStats() == null) {
            plugin.getLogger().log(Level.SEVERE,
                    "[StatManager] Fallo al cargar RPGStats para: " + player.getName());
            return;
        }

        // Aplicar visuales iniciales
        updatePlayerVisuals(player);

        // Añadir BossBar de estamina
        staminaBossBarManager.addStaminaBossBar(player);

        // Asegurar regeneración activa
        regenerationScheduler.ensurePlayerTask(uuid);
    }

    /* ---------------------------
       ACCESOS / HELPERS
       --------------------------- */
    public AttributeApplier getAttributeApplier() {
        return attributeApplier;
    }

    public LevelingManager getLevelingManager() {
        return levelingManager;
    }

    public StaminaBossBarManager getStaminaBossBarManager() {
        return staminaBossBarManager;
    }

    public RPGStats getStats(UUID uuid) {
        PlayerData data = plugin.getAuthManager().getPlayerData(uuid);
        if (data == null) {
            plugin.getLogger().log(Level.WARNING,
                    "[StatManager] RPGStats no encontrada para UUID: " + uuid);
            return null;
        }
        return data.getRpgStats();
    }

    public Main getPlugin() {
        return plugin;
    }

    /* ---------------------------
       VISUALES
       --------------------------- */
    public void updatePlayerVisuals(Player player) {
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            return; // No aplicar si no está logueado
        }

        RPGStats stats = getStats(player.getUniqueId());
        if (stats == null) return;

        // Recalcular bonos de equipo
        attributeApplier.recalculateEquippedBonuses(player);

        // Aplicar atributos vanilla
        attributeApplier.applyPlayerAttributes(player, stats);

        // Ocultar barra vanilla
        hideVanillaHealthTrick(player, stats);

        // Actualizar BossBar de estamina
        staminaBossBarManager.updateStaminaBossBar(player, stats);
    }

    private void hideVanillaHealthTrick(Player player, RPGStats stats) {
        double rpgMaxHealth = stats.getMaxHealth();
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        if (rpgMaxHealth <= 20.0) {
            attr.setBaseValue(20.0);
            player.setHealth(Math.min(20.0, stats.getCurrentHealth()));
            return;
        }

        double currentHealth = stats.getCurrentHealth();

        attr.setBaseValue(HIDE_VANILLA_HEALTH_VALUE);

        double vanillaCurrentHealth = player.getHealth();
        player.setHealth(Math.max(1.0, vanillaCurrentHealth - 0.0001));
        player.setHealth(vanillaCurrentHealth);

        attr.setBaseValue(rpgMaxHealth);
        player.setHealth(Math.min(rpgMaxHealth, currentHealth));
    }

    /* ---------------------------
       DELEGACIONES
       --------------------------- */
    public double getRpgMaxHealth(UUID uuid) {
        RPGStats stats = getStats(uuid);
        return stats != null ? attributeApplier.getRpgMaxHealth(stats) : RPGStats.BASE_HEALTH;
    }

    public void applyPlayerAttributes(Player player, RPGStats stats) {
        attributeApplier.applyPlayerAttributes(player, stats);
    }

    public void addStaminaBossBar(Player player) {
        staminaBossBarManager.addStaminaBossBar(player);
    }

    public void updateStaminaBossBar(Player player, RPGStats stats) {
        staminaBossBarManager.updateStaminaBossBar(player, stats);
    }

    public void removeStaminaBossBar(Player player) {
        staminaBossBarManager.removeStaminaBossBar(player);
    }

    public void startRegenScheduler() {
        regenerationScheduler.startRegenScheduler();
    }

    public void stopRegenScheduler() {
        regenerationScheduler.stopRegenScheduler();
    }

    public void openStatMenu(Player player) {
        try {
            menuBuilder.openStatMenu(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al abrir menú de stats para " + player.getName(), e);
        }
    }

    public void savePlayerStats(UUID uuid) {
        try {
            PlayerData data = plugin.getAuthManager().getPlayerData(uuid);
            if (data != null) {
                plugin.getUserDataManager().savePlayerData(data);
            } else {
                plugin.getLogger().warning("[StatManager] PlayerData es null para UUID: " + uuid);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar stats para UUID: " + uuid, e);
        }
    }

    /* ---------------------------
       COMANDOS
       --------------------------- */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando.").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(Component.text("Debes iniciar sesión para ver tus atributos.").color(NamedTextColor.RED));
            return true;
        }

        if (label.equalsIgnoreCase("stats") || label.equalsIgnoreCase("atributos")) {
            openStatMenu(player);
            return true;
        }
        return false;
    }

    /* ---------------------------
       MISC
       --------------------------- */
    public Map<UUID, BossBar> getStaminaBars() {
        return staminaBars;
    }

    public Map<UUID, Long> getStaminaCooldowns() {
        return staminaCooldowns;
    }

    public void stop() {
        stopRegenScheduler();
    }

    public void recalculateEquippedBonuses(Player player) {
        attributeApplier.recalculateEquippedBonuses(player);
        updatePlayerVisuals(player);
    }

    public boolean isInStaminaCooldown(UUID uuid) {
        Long until = staminaCooldowns.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

}
