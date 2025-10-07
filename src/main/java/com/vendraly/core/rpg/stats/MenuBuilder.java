package com.vendraly.core.rpg.stats;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.ability.AbilityType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menú de atributos RPG: muestra stats base, stats extendidos y skills pasivas.
 * - Marca PDC en cada item de menú para evitar que lo modifique el ItemLoreUpdater externo.
 * - Lore con “actual” y “próximo nivel” usando las constantes de RPGStats.
 */
public class MenuBuilder {

    private final Main plugin;
    private final StatManager statManager;
    private final AttributeApplier attributeApplier;
    private final LevelingManager levelingManager;

    // Clave PDC para marcar ítems del menú (ignorar por sistemas externos de lore)
    private final NamespacedKey MENU_ITEM_KEY;

    // Constantes visuales para stats extendidos (solo para mostrar)
    private static final double BASE_MC_SPEED = 0.1;               // Atributo vanilla de velocidad base
    private static final double SPEED_IMPROVEMENT_PER_POINT = 0.0005;
    private static final double BASE_HARVEST_PENALTY = -0.50;      // -50% base (se compensa con puntos)
    private static final double HARVEST_IMPROVEMENT_PER_POINT = 0.01; // +1% por punto

    public MenuBuilder(Main plugin, StatManager statManager,
                       AttributeApplier attributeApplier,
                       LevelingManager levelingManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.attributeApplier = attributeApplier;
        this.levelingManager = levelingManager;
        this.MENU_ITEM_KEY = new NamespacedKey(plugin, "RPG_MENU_ITEM");
    }

    /**
     * Abre el menú principal de estadísticas RPG para el jugador.
     */
    public void openStatMenu(Player player) {
        RPGStats stats = statManager.getStats(player.getUniqueId());
        if (stats == null) return;

        // Recalcular bonos de equipo antes de mostrar
        attributeApplier.recalculateEquippedBonuses(player);

        Inventory inv = Bukkit.createInventory(
                player,
                27, // 3 filas x 9
                Component.text("Atributos RPG").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD)
        );

        // --- Colocación de items ---
        // Fila 0: siete stats “core”
        int[] row0 = {10, 11, 12, 13, 14, 15, 16};
        inv.setItem(row0[0], createStatItem(Material.RED_DYE,          "Vida Máxima",           stats.getStatHealth(),        stats));
        inv.setItem(row0[1], createStatItem(Material.DIAMOND_SWORD,    "Fuerza",                stats.getStatStrength(),      stats));
        inv.setItem(row0[2], createStatItem(Material.IRON_CHESTPLATE,  "Defensa",               stats.getStatDefense(),       stats));
        inv.setItem(row0[3], createStatItem(Material.LEATHER_BOOTS,    "Agilidad",              stats.getStatAgility(),       stats));
        inv.setItem(row0[4], createStatItem(Material.COOKED_BEEF,      "Estamina Máxima",       stats.getStatStaminaMax(),    stats));
        inv.setItem(row0[5], createStatItem(Material.POTION,           "Regeneración Estamina", stats.getStatStaminaRegen(),  stats));
        inv.setItem(row0[6], createStatItem(Material.APPLE,            "Regeneración Vida",     stats.getStatHealthRegen(),   stats));

        // Fila 1 (stats extendidos): movimiento/minado/talado
        inv.setItem(19, createStatItem(Material.RABBIT_FOOT,  "Velocidad Movimiento", stats.getStatMovementSpeed(),   stats));
        inv.setItem(20, createStatItem(Material.DIAMOND_PICKAXE, "Velocidad Minado",  stats.getStatMiningSpeed(),     stats));
        inv.setItem(21, createStatItem(Material.DIAMOND_AXE,     "Velocidad Talado",  stats.getStatWoodcuttingSpeed(),stats));

        // Fila 1-2 (skills pasivas)
        inv.setItem(23, createSkillItem(Material.ANVIL,             "Herrería",  AbilityType.valueOf(StatManager.BLACKSMITHING), stats));
        inv.setItem(24, createSkillItem(Material.LEATHER_CHESTPLATE,"Sastrería", AbilityType.valueOf(StatManager.TAILORING),     stats));
        inv.setItem(25, createSkillItem(Material.BREWING_STAND,     "Boticario", AbilityType.valueOf(StatManager.APOTHECARY),    stats));

        // Centro: nivel y puntos disponibles
        inv.setItem(4, createPointsItem(stats));

        player.openInventory(inv);
    }

    // ------------------------------------------------------------------------
    // Creación de ítems
    // ------------------------------------------------------------------------

    private ItemStack createPointsItem(RPGStats stats) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Puntos de Habilidad").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));

        List<Component> lore = Arrays.asList(
                Component.empty(),
                Component.text("Nivel: ").append(Component.text(stats.getLevel()).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)),
                Component.text("Puntos Restantes: ").append(Component.text(stats.getUnspentPoints()).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)),
                Component.empty(),
                Component.text("Usa /stats o /atributos para abrir este menú.")
                        .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
        );

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatItem(Material material, String name, int level, RPGStats stats) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Mejorar ").append(Component.text(name).color(NamedTextColor.GOLD)));

        List<Component> lore = new ArrayList<>(createStatLore(name, level, stats));
        lore.add(Component.empty());

        if (stats.getUnspentPoints() > 0) {
            lore.add(Component.text("Click Izquierdo: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("Subir Nivel (+1)").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)));
        } else {
            lore.add(Component.text("¡Necesitas más puntos!").color(NamedTextColor.RED));
        }

        meta.lore(lore);
        applyMenuItemTag(item, meta);
        return item;
    }

    private ItemStack createSkillItem(Material material, String displayName, AbilityType ability, RPGStats stats) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Habilidad: ").color(NamedTextColor.DARK_AQUA)
                .append(Component.text(displayName).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));

        // Usamos el nombre del enum como clave en RPGStats (string)
        String key = ability.name();
        int level = stats.getSkillLevel(key);
        long curXP = stats.getSkillExp(key);
        long reqXP = RPGStats.getExpForNextLevel(level);
        float progress = (reqXP > 0) ? Math.min(0.999f, (float) curXP / (float) reqXP) : 0f;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Nivel Actual: ").append(Component.text(level).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)));
        lore.add(Component.text("EXP Actual: ").append(Component.text(String.format("%,d", curXP)).color(NamedTextColor.YELLOW)));
        lore.add(Component.text("Requerida para ").append(Component.text("Nv. " + (level + 1)).color(NamedTextColor.AQUA))
                .append(Component.text(": ")).append(Component.text(String.format("%,d", reqXP)).color(NamedTextColor.GREEN)));
        lore.add(Component.text("Progreso: ").append(Component.text(String.format("%.1f%%", progress * 100)).color(NamedTextColor.GREEN)));
        lore.add(Component.empty());
        lore.add(Component.text(switch (ability) {
            case BLACKSMITHING -> "Mejora armas y herramientas crafteadas.";
            case TAILORING     -> "Mejora armaduras de tela y cuero crafteadas.";
            case APOTHECARY    -> "Aumenta efectividad y duración de pociones.";
            default            -> "Habilidad pasiva de progresión.";
        }).color(NamedTextColor.GRAY));

        meta.lore(lore);
        applyMenuItemTag(item, meta);
        return item;
    }

    private void applyMenuItemTag(ItemStack item, ItemMeta meta) {
        meta.getPersistentDataContainer().set(MENU_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------------
    // Lore dinámico por atributo
    // ------------------------------------------------------------------------
    private List<Component> createStatLore(String statName, int investedPoints, RPGStats stats) {
        switch (statName) {
            case "Vida Máxima" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("HP Máximos: ").append(Component.text(String.format("%.1f", stats.getMaxHealth())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.HEALTH_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" HP"))
                );
            }
            case "Fuerza" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Daño: ").append(Component.text(String.format("%.1f", stats.getAttackPower())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.STRENGTH_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" Daño"))
                );
            }
            case "Defensa" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Reducción: ").append(Component.text(String.format("%.1f", stats.getDefensePower())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.DEFENSE_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" Reducción"))
                );
            }
            case "Agilidad" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Bonificación Robos: ").append(Component.text(String.format("%.1f pts", stats.getScaledAgilityBonus())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.AGILITY_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" pts"))
                );
            }
            case "Estamina Máxima" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Estamina: ").append(Component.text(String.format("%.0f", stats.getMaxStamina())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.STAMINA_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" puntos"))
                );
            }
            case "Regeneración Estamina" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Regeneración: ").append(Component.text(String.format("%.1f/s", stats.getStaminaRegenPerSecond())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.STAMINA_REGEN_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" /s"))
                );
            }
            case "Regeneración Vida" -> {
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Regeneración: ").append(Component.text(String.format("%.2f HP/s", stats.getHealthRegenPerSecond())).color(NamedTextColor.GREEN)),
                        Component.text("Próximo Nivel: +").append(Component.text(RPGStats.HEALTH_REGEN_PER_POINT).color(NamedTextColor.YELLOW)).append(Component.text(" HP/s"))
                );
            }
            case "Velocidad Movimiento" -> {
                double current = BASE_MC_SPEED + (stats.getStatMovementSpeed() * SPEED_IMPROVEMENT_PER_POINT);
                double next    = BASE_MC_SPEED + ((stats.getStatMovementSpeed() + 1) * SPEED_IMPROVEMENT_PER_POINT);
                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Velocidad Atributo: ").append(Component.text(String.format("%.4f", current)).color(NamedTextColor.GREEN))
                                .append(Component.text(" (base 0.1000)").color(NamedTextColor.GRAY)),
                        Component.text("Próximo Nivel: ").append(Component.text(String.format("%.4f", next)).color(NamedTextColor.YELLOW))
                );
            }
            case "Velocidad Minado", "Velocidad Talado" -> {
                int pts = statName.contains("Minado") ? stats.getStatMiningSpeed() : stats.getStatWoodcuttingSpeed();

                double mult   = BASE_HARVEST_PENALTY + pts * HARVEST_IMPROVEMENT_PER_POINT;     // -0.50 + 0.01 * pts
                mult = Math.max(BASE_HARVEST_PENALTY, mult);
                double pct    = (1.0 + mult) * 100.0; // en %

                double nextM  = BASE_HARVEST_PENALTY + (pts + 1) * HARVEST_IMPROVEMENT_PER_POINT;
                double nextPc = (1.0 + nextM) * 100.0;

                return Arrays.asList(
                        investedLine(investedPoints),
                        Component.text("Velocidad: ").append(Component.text(String.format("%.1f%%", pct)).color(NamedTextColor.GREEN))
                                .append(Component.text(" (base 50%)").color(NamedTextColor.GRAY)),
                        Component.text("Próximo Nivel: ").append(Component.text(String.format("%.1f%%", nextPc)).color(NamedTextColor.YELLOW))
                );
            }
            default -> {
                return Arrays.asList(Component.text("ERROR: Atributo no encontrado.").color(NamedTextColor.RED));
            }
        }
    }

    private Component investedLine(int invested) {
        return Component.text("Puntos Invertidos: ").append(
                Component.text(invested).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)
        );
    }
}
