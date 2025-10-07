package com.vendraly.core.rpg.ability;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;

/**
 * Listener que maneja la habilidad de Herrería (Blacksmithing).
 * Otorga experiencia al jugador al craftear ítems elegibles
 * y notifica con la calidad del ítem.
 */
public class BlacksmithingListener implements Listener {

    private final AbilityManager abilityManager;
    private final ItemMetadataKeys keys;

    private static final AbilityType SKILL_TYPE = AbilityType.BLACKSMITHING;

    // Materiales que otorgan EXP de Herrería
    private static final EnumSet<Material> AFFECTED_MATERIALS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_SHOVEL, Material.WOODEN_HOE,
            Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL, Material.STONE_HOE,
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE,
            Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE,
            Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
            Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE
    );

    public BlacksmithingListener(Main plugin) {
        this.abilityManager = plugin.getAbilityManager();
        this.keys = plugin.getItemMetadataKeys();
    }

    /**
     * Maneja el evento de crafteo para la habilidad de Herrería.
     * Otorga EXP y notifica al jugador con calidad y color.
     */
    @EventHandler
    public void onCraftBlacksmithItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack resultItem = event.getRecipe().getResult();
        if (resultItem == null || !AFFECTED_MATERIALS.contains(resultItem.getType())) {
            return;
        }

        // --- 1. Calcular EXP base ---
        boolean canGainExp = abilityManager.canGainExp(player, SKILL_TYPE);
        int baseExp = getExperienceForRecipe(resultItem.getType());

        int amountCrafted = event.isShiftClick()
                ? event.getInventory().getMatrix().length // simplificado: slots consumidos
                : resultItem.getAmount();

        long totalExp = (long) baseExp * amountCrafted;

        // --- 2. Obtener calidad del ítem (si fue establecida en el PDC) ---
        String quality = "COMUN";
        NamedTextColor qualityColor = NamedTextColor.WHITE;

        ItemMeta meta = resultItem.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(keys.ITEM_QUALITY, PersistentDataType.STRING)) {
                quality = pdc.get(keys.ITEM_QUALITY, PersistentDataType.STRING);
                qualityColor = getColorForQuality(quality);
            }
        }

        // --- 3. Aplicar EXP y notificar ---
        if (canGainExp) {
            abilityManager.addExp(player, SKILL_TYPE, (int) totalExp);

            Component message = Component.text("[HERRERÍA] +", NamedTextColor.GREEN)
                    .append(Component.text(totalExp, NamedTextColor.YELLOW))
                    .append(Component.text(" EXP. Creaste un ítem "))
                    .append(Component.text(quality, qualityColor, TextDecoration.BOLD));

            player.sendMessage(message);
        } else {
            player.sendMessage(Component.text("[Herrería] Has alcanzado el nivel máximo.", NamedTextColor.RED, TextDecoration.ITALIC));
        }
    }

    // --- Métodos utilitarios ---
    private int getExperienceForRecipe(Material material) {
        String name = material.name();
        if (name.contains("NETHERITE")) return 100;
        if (name.contains("DIAMOND")) return 50;
        if (name.contains("IRON")) return 25;
        if (name.contains("GOLDEN")) return 20;
        if (name.contains("STONE")) return 10;
        if (name.contains("WOODEN")) return 5;
        return 5;
    }

    private NamedTextColor getColorForQuality(String quality) {
        if (quality == null) return NamedTextColor.GRAY;
        switch (quality.toUpperCase()) {
            case "COMUN": return NamedTextColor.WHITE;
            case "RARO": return NamedTextColor.GREEN;
            case "EPICO": return NamedTextColor.LIGHT_PURPLE;
            case "LEGENDARIO": return NamedTextColor.GOLD;
            case "MITICO": return NamedTextColor.RED;
            default: return NamedTextColor.GRAY;
        }
    }
}
