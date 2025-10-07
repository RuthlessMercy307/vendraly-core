package com.vendraly.core.rpg.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

/**
 * Utilidad estática para leer y escribir valores en el PersistentDataContainer (PDC)
 * de cualquier ItemStack de forma segura y sin tanto boilerplate.
 */
public class PDCUtils {

    // --------------------
    // --- MÉTODOS GET ---
    // --------------------

    public static int getInt(ItemStack item, NamespacedKey key, int defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(key, PersistentDataType.INTEGER, defaultValue);
    }

    public static double getDouble(ItemStack item, NamespacedKey key, double defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(key, PersistentDataType.DOUBLE, defaultValue);
    }

    public static String getString(ItemStack item, NamespacedKey key, String defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(key, PersistentDataType.STRING, defaultValue);
    }

    public static boolean getBoolean(ItemStack item, NamespacedKey key) {
        return getInt(item, key, 0) == 1;
    }

    // --------------------
    // --- MÉTODOS SET ---
    // --------------------

    public static void setInt(ItemStack item, NamespacedKey key, int value) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    public static void setDouble(ItemStack item, NamespacedKey key, double value) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
    }

    public static void setString(ItemStack item, NamespacedKey key, String value) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    public static void setBoolean(ItemStack item, NamespacedKey key, boolean value) {
        setInt(item, key, value ? 1 : 0);
    }

    // ----------------------------
    // --- MÉTODOS DE UTILIDAD ---
    // ----------------------------

    public static boolean has(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key);
    }

    public static void remove(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
    }
}
