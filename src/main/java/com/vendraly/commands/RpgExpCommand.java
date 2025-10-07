package com.vendraly.commands;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.RPGStats;
import com.vendraly.core.rpg.ability.AbilityType; // Importación necesaria para el nuevo caso
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de Administración para modificar estadísticas RPG: /rgpexp <jugador> <add|remove|set> <cantidad> <lvl|points|stat|ability> [target_name]
 */
public class RpgExpCommand implements CommandExecutor {

    private final Main plugin;

    public RpgExpCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("vendraly.rpg.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        // Sintaxis mínima: /rgpexp <jugador> <add|set|remove> <cantidad> <target>
        if (args.length < 4) {
            sendUsage(sender);
            return true;
        }

        // 1. Obtener Jugador
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + args[0] + "' no está en línea.");
            return true;
        }

        // Obtener Stats
        RPGStats stats = plugin.getStatManager().getStats(target.getUniqueId());
        if (stats == null) {
            sender.sendMessage(ChatColor.RED + "No se pudieron cargar las estadísticas de " + target.getName());
            return true;
        }

        // 2. Parsear Acción, Cantidad y Objetivo
        String action = args[1].toLowerCase(); // add, remove, set
        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (action.equals("add") && amount < 0) amount = -amount;
            if (action.equals("remove")) amount = -Math.abs(amount); // Remove siempre es negativo
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La cantidad debe ser un número entero válido.");
            return true;
        }

        String targetType = args[3].toLowerCase(); // lvl, points, stat, ability
        String targetName = args.length > 4 ? args[4].toLowerCase() : null;

        boolean success = false;
        String statDisplayName = "";
        long finalValue = -1; // Valor a mostrar al final

        switch (targetType) {
            case "lvl":
            case "level":
                int currentLevel = stats.getLevel();
                int newLevel = (int) (action.equals("set") ? amount : currentLevel + amount);
                stats.setLevel(Math.max(1, newLevel));
                stats.setTotalExperience(0);
                success = true;
                statDisplayName = "Nivel RPG";
                finalValue = stats.getLevel();
                break;

            case "points":
            case "puntos":
                int currentPoints = stats.getUnspentPoints();
                int newPoints = (int) (action.equals("set") ? amount : currentPoints + amount);
                stats.setUnspentPoints(Math.max(0, newPoints));
                success = true;
                statDisplayName = "Puntos no gastados";
                finalValue = stats.getUnspentPoints();
                break;

            case "stat":
            case "perk":
            case "peerks":
                if (targetName == null) {
                    sender.sendMessage(ChatColor.RED + "Falta el nombre del atributo (stat_name).");
                    return true;
                }

                success = stats.setStatValueAdmin(targetName, (int) amount, action);
                if (success) {
                    statDisplayName = "Puntos en Stat " + targetName.toUpperCase();
                    // Recuperar el valor final (asume que getCurrentValueForTarget maneja 'stat')
                    finalValue = stats.getCurrentValueForTarget(targetType, targetName);
                } else {
                    sender.sendMessage(ChatColor.RED + "Atributo '" + targetName + "' no reconocido o no se pudo modificar.");
                    return true;
                }
                break;

            case "ability":
            case "skill":
                // --- NUEVA LÓGICA PARA HABILIDADES SECUNDARIAS ---
                if (targetName == null) {
                    sender.sendMessage(ChatColor.RED + "Falta el nombre de la habilidad. Ej: Blacksmithing o Tailoring.");
                    return true;
                }

                String abilityNameUpper = targetName.toUpperCase();

                // ASUME: Tienes el AbilityManager en tu plugin
                try {
                    AbilityType type = AbilityType.valueOf(abilityNameUpper);

                    // Llama al setter administrativo en AbilityManager
                    // ASUME: plugin.getAbilityManager().setAbilityLevelAdmin(target.getUniqueId(), type, (int) amount, action) existe
                    success = plugin.getAbilityManager().setAbilityLevelAdmin(target.getUniqueId(), type, (int) amount, action);

                    if (success) {
                        statDisplayName = "Nivel en Habilidad " + type.getDisplayName(); // Asume que AbilityType tiene getDisplayName()
                        finalValue = stats.getAbilityLevel(type); // Asume que RPGStats tiene este getter
                    } else {
                        sender.sendMessage(ChatColor.RED + "No se pudo modificar la habilidad (posiblemente la acción es inválida).");
                        return true;
                    }

                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Habilidad '" + targetName + "' no reconocida. Usa Blacksmithing, Tailoring, etc.");
                    return true;
                }
                break;

            default:
                sendUsage(sender);
                return true;
        }

        // 3. Notificación
        sender.sendMessage(ChatColor.GREEN + "Estadísticas de " + target.getName() + " actualizadas:");

        if (finalValue != -1) {
            if (action.equals("set")) {
                sender.sendMessage(ChatColor.GREEN + " -> " + statDisplayName + " establecido a " + finalValue + ".");
            } else {
                sender.sendMessage(ChatColor.GREEN + " -> Se ha " + (amount >= 0 ? "añadido" : "removido") + " " + Math.abs(amount) + " a " + statDisplayName + ". Valor final: " + finalValue);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Advertencia: El valor final no pudo ser recuperado.");
        }

        // 4. ACTUALIZACIÓN VISUAL CRÍTICA
        plugin.getStatManager().updatePlayerVisuals(target);

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "--- Uso del Comando /rgpexp ---");
        sender.sendMessage(ChatColor.YELLOW + "/rgpexp <jugador> <add|remove|set> <cantidad> <lvl|points>");
        sender.sendMessage(ChatColor.YELLOW + "/rgpexp <jugador> <add|remove|set> <cantidad> stat <nombre_atributo>");
        sender.sendMessage(ChatColor.YELLOW + "/rgpexp <jugador> <add|remove|set> <cantidad> ability <nombre_habilidad>");
        sender.sendMessage(ChatColor.AQUA + "Ejemplos:");
        sender.sendMessage(ChatColor.AQUA + "/rgpexp vendraly set 50 lvl");
        sender.sendMessage(ChatColor.AQUA + "/rgpexp vendraly add 10 points");
        sender.sendMessage(ChatColor.AQUA + "/rgpexp vendraly set 50 stat strength");
        sender.sendMessage(ChatColor.AQUA + "/rgpexp vendraly set 50 ability blacksmithing");
    }
}