package com.vendraly.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Utilidad ligera para ejecutar tareas sincronas en Bukkit.
 */
public final class TaskUtil {

    private TaskUtil() {
    }

    public static void runLater(Plugin plugin, Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static void runTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }

    public static void runSync(Plugin plugin, Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
