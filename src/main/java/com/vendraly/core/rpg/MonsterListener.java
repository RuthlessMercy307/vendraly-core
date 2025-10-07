package com.vendraly.core.rpg;

import com.vendraly.core.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class MonsterListener implements Listener {

    private final Main plugin;
    private final WorldDifficultyManager difficultyManager;
    private static final String RPG_METADATA_KEY = "RPG_MONSTER_DATA";
    private static final String PROCESSED_METADATA_KEY = "RPG_LEVEL_SET";

    public MonsterListener(Main plugin) {
        this.plugin = plugin;
        this.difficultyManager = plugin.getDifficultyManager();
    }

    public RPGMonster getRpgMonster(LivingEntity entity) {
        if (entity.hasMetadata(RPG_METADATA_KEY)) {
            return (RPGMonster) entity.getMetadata(RPG_METADATA_KEY).get(0).value();
        }
        return null;
    }

    // ===============================================
    // SPAWN NATURAL: Asignación de nivel y atributos
    // ===============================================
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entityBase = event.getEntity();

        if (event.isCancelled() || !(entityBase instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) entityBase;

        if (entity.hasMetadata(PROCESSED_METADATA_KEY)) return;
        if (entity instanceof Player || entity.hasMetadata(RPG_METADATA_KEY)) return;

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            applyRpgAttributes(entity, 1);
            entity.setMetadata(PROCESSED_METADATA_KEY, new FixedMetadataValue(plugin, true));
            return;
        }

        int level = difficultyManager.getSpawnLevel(event.getLocation());
        applyRpgAttributes(entity, level);
        entity.setMetadata(PROCESSED_METADATA_KEY, new FixedMetadataValue(plugin, true));
    }

    /**
     * Método público para que ZoneSpawner pueda aplicar stats RPG a mobs spawneados.
     */
    public void applyRpgAttributesPublic(LivingEntity entity, int level) {
        if (entity.hasMetadata(PROCESSED_METADATA_KEY) || entity.hasMetadata(RPG_METADATA_KEY)) {
            return;
        }
        applyRpgAttributes(entity, level);
        entity.setMetadata(PROCESSED_METADATA_KEY, new FixedMetadataValue(plugin, true));
    }

    /**
     * Lógica centralizada para aplicar los atributos RPG.
     */
    private void applyRpgAttributes(LivingEntity entity, int level) {
        RPGMonster rpgMob = new RPGMonster(entity, level);
        rpgMob.applyAttributes();
        entity.setMetadata(RPG_METADATA_KEY, new FixedMetadataValue(plugin, rpgMob));

        Component baseName = entity.name();
        Component nameTag = Component.text("[Lv. " + level + "] ")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                .append(baseName);

        entity.customName(nameTag);
        entity.setCustomNameVisible(true);
    }

    // ===============================================
    // COMBATE: Reducción de daño
    // ===============================================
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entityBase = event.getEntity();
        if (!(entityBase instanceof LivingEntity)) return;
        LivingEntity damagedEntity = (LivingEntity) entityBase;

        RPGMonster rpgMob = getRpgMonster(damagedEntity);
        if (rpgMob != null) {
            double defense = rpgMob.getDefenseReduction();
            double originalDamage = event.getDamage();
            double finalDamage = Math.max(0.1, originalDamage - defense);

            event.setDamage(finalDamage);

            if (event.getDamager() instanceof Player attacker && originalDamage > 0) {
                attacker.sendActionBar(Component.text("Daño: ").color(NamedTextColor.RED)
                        .append(Component.text(String.format("%.1f", originalDamage)).color(NamedTextColor.YELLOW))
                        .append(Component.text(" -> ").color(NamedTextColor.GRAY))
                        .append(Component.text(String.format("%.1f", finalDamage)).color(NamedTextColor.RED))
                        .append(Component.text(" (Def: ").color(NamedTextColor.GRAY))
                        .append(Component.text(String.format("%.1f", defense)).color(NamedTextColor.AQUA))
                        .append(Component.text(")").color(NamedTextColor.GRAY)));
            }
        }
    }

    // ===============================================
    // MUERTE: Otorgar EXP custom
    // ===============================================
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        RPGMonster rpgMob = getRpgMonster(entity);
        if (rpgMob == null || killer == null) return;

        // 1. BLOQUEAR la experiencia vanilla.
        event.setDroppedExp(0);

        // 2. EXP personalizada
        long expToGive = rpgMob.getExpReward();
        if (plugin.getXPManager() != null) {
            plugin.getXPManager().addExp(killer, expToGive);
        } else {
            plugin.getLogger().warning("XPManager no inicializado, no se pudo otorgar EXP.");
        }

        // 3. ORO personalizado
        double goldReward = rpgMob.getLevel() * 5.0; // 💰 Fórmula de recompensa
        if (plugin.getCashManager() != null) {
            plugin.getCashManager().modifyCash(killer.getUniqueId(), goldReward).thenAccept(success -> {
                if (success) {
                    // Mensajes visuales al jugador (en el hilo principal)
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        killer.sendActionBar(Component.text("+ " + expToGive + " EXP").color(NamedTextColor.AQUA)
                                .append(Component.text(" | + " + goldReward + " oro").color(NamedTextColor.GOLD)));
                    });
                } else {
                    plugin.getLogger().warning("No se pudo otorgar oro a " + killer.getName());
                }
            });
        } else {
            plugin.getLogger().warning("CashManager no inicializado, no se pudo otorgar oro.");
        }

        // 4. Limpieza de Metadata
        entity.removeMetadata(RPG_METADATA_KEY, plugin);
        entity.removeMetadata(PROCESSED_METADATA_KEY, plugin);
    }

}
