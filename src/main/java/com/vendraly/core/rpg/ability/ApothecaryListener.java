package com.vendraly.core.rpg.ability;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.StatManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener que maneja la habilidad de Alquimia (Apothecary).
 * Esta habilidad otorga experiencia y aplica bonificaciones de
 * Mejora de Pociones (Potion Power) al preparar pociones.
 *
 * NOTA IMPORTANTE: La EXP y la bonificación final con el nivel correcto
 * DEBE manejarse a través de un Custom Brew Handler que llame a
 * grantExpAndNotify cuando el jugador recoge la poción.
 */
public class ApothecaryListener implements org.bukkit.event.Listener {

    private final Main plugin;
    private final AbilityManager abilityManager;
    private final StatManager statManager;

    private static final AbilityType SKILL_TYPE = AbilityType.APOTHECARY;

    // Cantidad base de experiencia por preparación (Brew)
    private static final int BASE_EXP_PER_BREW = 20;

    // Stat clave de bonificación en el Lore
    private static final String POTION_POWER_LORE_PREFIX = "Poder de Poción: ";
    private static final String APOTHECARY_LORE_TAG = "Habilidad: Alquimia (Poder)";

    public ApothecaryListener(Main plugin) {
        this.plugin = plugin;
        this.abilityManager = plugin.getAbilityManager();
        this.statManager = plugin.getStatManager();
    }

    /**
     * Verifica si el ItemStack es un tipo de poción (normal, splash o lingering).
     */
    private boolean isPotionItem(ItemStack item) {
        return item != null && (
                item.getType() == Material.POTION ||
                        item.getType() == Material.SPLASH_POTION ||
                        item.getType() == Material.LINGERING_POTION
        );
    }

    /**
     * Aplica la bonificación de Poder de Poción a la poción preparada.
     * Esto incluye modificar los efectos reales y el Lore.
     */
    public ItemStack applyApothecaryBonus(ItemStack item, int apothecaryLevel) {
        if (!isPotionItem(item)) return item;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return item;

        // Bonificación de Poder de Poción: 0.5% extra por nivel
        double potionPowerBonusPercent = apothecaryLevel * 0.5;
        double multiplier = 1.0 + (potionPowerBonusPercent / 100.0);

        // --- 1. MODIFICAR EFECTOS ---
        if (meta.hasCustomEffects()) {
            List<PotionEffect> modified = new ArrayList<>();
            for (PotionEffect effect : meta.getCustomEffects()) {
                int newDuration = (int) Math.round(effect.getDuration() * multiplier);
                modified.add(new PotionEffect(
                        effect.getType(),
                        newDuration,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon()
                ));
            }
            meta.clearCustomEffects();
            for (PotionEffect effect : modified) {
                meta.addCustomEffect(effect, true);
            }
        }

        // --- 2. MODIFICAR LORE Y NOMBRE ---
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;

        // Nombre visible solo si nivel > 0
        if (apothecaryLevel > 0) {
            String rawName = meta.getBasePotionType() != null
                    ? meta.getBasePotionType().name()
                    : item.getType().name();

            String basePotionName = capitalizeWords(rawName.replace('_', ' '));
            itemMeta.setDisplayName(
                    ChatColor.GREEN + "" + ChatColor.BOLD + basePotionName + " Mejorada"
                            + ChatColor.DARK_GRAY + " (Nivel " + apothecaryLevel + ")"
            );
        }

        // Limpiar lore anterior
        List<String> newLore = new ArrayList<>();
        List<String> oldLore = itemMeta.getLore();
        if (oldLore != null) {
            for (String line : oldLore) {
                String rawText = ChatColor.stripColor(line);
                if (!rawText.contains(APOTHECARY_LORE_TAG) && !rawText.contains(POTION_POWER_LORE_PREFIX)) {
                    newLore.add(line);
                }
            }
        }

        // Añadir nueva info de poder de poción
        if (potionPowerBonusPercent > 0) {
            newLore.add("");
            newLore.add(ChatColor.GRAY + POTION_POWER_LORE_PREFIX
                    + ChatColor.GREEN + "" + ChatColor.BOLD
                    + "+" + String.format("%.1f", potionPowerBonusPercent) + "%");
            newLore.add("");
            newLore.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + APOTHECARY_LORE_TAG);
        }

        itemMeta.setLore(newLore);
        item.setItemMeta(itemMeta);

        return item;
    }

    /**
     * Otorga EXP de Alquimia y notifica.
     * Se debe llamar desde un Custom Brew Handler que identifique al jugador.
     */
    public void grantExpAndNotify(Player player, int playerLevel) {
        if (!abilityManager.canGainExp(player, SKILL_TYPE)) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "[Alquimia] Has alcanzado el nivel máximo.");
            return;
        }

        int expGained = BASE_EXP_PER_BREW * 3;
        abilityManager.addExp(player, SKILL_TYPE, expGained);

        double bonusApplied = playerLevel * 0.5;
        player.sendActionBar(ChatColor.GREEN + "[Alquimia] +" + ChatColor.WHITE + expGained
                + " EXP " + ChatColor.GRAY + "| " + ChatColor.GREEN
                + "Poder de Poción: +" + String.format("%.1f", bonusApplied) + "% aplicado.");
    }

    /**
     * Convierte texto como 'JUMP_BOOST' a 'Jump Boost'.
     */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1)).append(" ");
        }
        return result.toString().trim();
    }
}
