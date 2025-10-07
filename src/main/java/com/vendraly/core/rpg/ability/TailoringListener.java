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
 * Listener para la habilidad de Sastrería (Tailoring).
 * Otorga experiencia al jugador al craftear armaduras elegibles
 * y notifica con la calidad del ítem.
 */
public class TailoringListener implements Listener {

    private final AbilityManager abilityManager;
    private final ItemMetadataKeys keys;

    private static final AbilityType SKILL_TYPE = AbilityType.TAILORING;

    // Materiales que otorgan EXP de Sastrería
    private static final EnumSet<Material> AFFECTED_MATERIALS = EnumSet.of(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
    );

    public TailoringListener(Main plugin, ItemMetadataKeys keys) {
        this.abilityManager = plugin.getAbilityManager();
        this.keys = keys;
    }

    /**
     * Maneja el evento de crafteo para la habilidad de Sastrería.
     * Otorga EXP según el material y notifica al jugador.
     */
    @EventHandler
    public void onCraftArmor(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack resultItem = event.getRecipe().getResult();
        if (resultItem == null || !AFFECTED_MATERIALS.contains(resultItem.getType())) {
            return;
        }

        // --- 1. Calcular EXP base ---
        boolean canGainExp = abilityManager.canGainExp(player, SKILL_TYPE);
        int baseExp = getExperienceForRecipe(resultItem.getType());

        // Cantidad de ítems realmente producidos
        int amountCrafted = event.isShiftClick()
                ? event.getInventory().getMatrix().length // simplificación: slots consumidos
                : resultItem.getAmount();

        long totalExp = (long) baseExp * amountCrafted;

        // --- 2. Obtener Calidad del ítem (si fue seteada en el RPGItemGenerator) ---
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

            Component message = Component.text("[SASTRERÍA] +", NamedTextColor.GREEN)
                    .append(Component.text(totalExp, NamedTextColor.YELLOW))
                    .append(Component.text(" EXP. Creaste un ítem "))
                    .append(Component.text(quality, qualityColor, TextDecoration.BOLD));

            player.sendMessage(message);
        } else {
            player.sendMessage(Component.text("[Sastrería] Has alcanzado el nivel máximo.", NamedTextColor.RED, TextDecoration.ITALIC));
        }
    }

    // --- Métodos utilitarios ---

    private int getExperienceForRecipe(Material material) {
        String name = material.name();
        if (name.contains("NETHERITE")) return 100;
        if (name.contains("DIAMOND")) return 60;
        if (name.contains("IRON")) return 30;
        if (name.contains("CHAINMAIL") || name.contains("GOLDEN")) return 20;
        if (name.contains("LEATHER")) return 15;
        return 10;
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
