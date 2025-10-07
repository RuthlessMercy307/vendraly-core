package com.vendraly.core.rpg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utilidad para calidades de ítems (durabilidad base, atributos, colores y lore).
 */
public class QualityUtility {

    public enum ItemQuality {
        COMUN("Común", NamedTextColor.GRAY, ChatColor.GRAY, 20, 1),
        RARO("Raro", NamedTextColor.BLUE, ChatColor.BLUE, 40, 2),
        EPICO("Épico", NamedTextColor.DARK_PURPLE, ChatColor.DARK_PURPLE, 60, 3),
        LEGENDARIO("Legendario", NamedTextColor.GOLD, ChatColor.GOLD, 80, 4),
        MITICO("Mítico", NamedTextColor.RED, ChatColor.RED, 100, 5);

        private final String name;
        private final NamedTextColor adventureColor;
        private final ChatColor bukkitColor;
        private final int durabilityPercent;
        private final int attributeCount;

        ItemQuality(String name, NamedTextColor adventureColor, ChatColor bukkitColor, int durabilityPercent, int attributeCount) {
            this.name = name;
            this.adventureColor = adventureColor;
            this.bukkitColor = bukkitColor;
            this.durabilityPercent = durabilityPercent;
            this.attributeCount = attributeCount;
        }

        public String getName() { return name; }
        public NamedTextColor getAdventureColor() { return adventureColor; }
        public ChatColor getBukkitColor() { return bukkitColor; }
        public int getDurabilityPercent() { return durabilityPercent; }
        public int getAttributeCount() { return attributeCount; }
    }

    // -------------------
    // Búsquedas
    // -------------------
    public static NamedTextColor getNamedColorForQuality(String quality) {
        return fromName(quality).getAdventureColor();
    }

    public static ChatColor getChatColorForQuality(String quality) {
        return fromName(quality).getBukkitColor();
    }

    public static int getDurabilityPercentForQuality(String quality) {
        return fromName(quality).getDurabilityPercent();
    }

    public static int getAttributeCountForQuality(String quality) {
        return fromName(quality).getAttributeCount();
    }

    public static ItemQuality fromName(String name) {
        for (ItemQuality q : ItemQuality.values()) {
            if (q.getName().equalsIgnoreCase(name)) return q;
        }
        return ItemQuality.COMUN;
    }

    // -------------------
    // Lectura desde item
    // -------------------
    public static String getQualityFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "Común";
        }
        String plainName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        String[] parts = plainName.trim().split(" ");
        return parts.length > 0 ? parts[0] : "Común";
    }

    // -------------------
    // Cálculo
    // -------------------
    public static String calculateQuality(int skillLevel, int difficultyLevel) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double successChance = (double) (skillLevel - (difficultyLevel * 0.5)) / (difficultyLevel * 1.5);

        if (successChance < 0.05) successChance = 0.05;
        if (successChance > 1.0) successChance = 1.0;

        String best = "Común";
        ItemQuality[] values = ItemQuality.values();

        for (int i = 0; i < values.length; i++) {
            double chance = successChance - (i * 0.25);
            if (chance < 0) chance = 0;
            if (random.nextDouble() < chance) {
                best = values[i].getName();
            } else break;
        }
        return best;
    }

    // -------------------
    // Presentación visual
    // -------------------
    public static Component getLoreLineForQuality(String quality) {
        ItemQuality q = fromName(quality);
        return Component.text("Calidad: " + q.getName())
                .color(q.getAdventureColor());
    }
}
