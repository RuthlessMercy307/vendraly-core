package com.vendraly.commands;

import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stats.StatType;
import com.vendraly.core.rpg.stats.XPManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Permite invertir puntos de estadísticas.
 */
public class StatsCommand implements CommandExecutorHolder {

    private final StatManager statManager;
    private final XPManager xpManager;

    public StatsCommand(StatManager statManager, XPManager xpManager) {
        this.statManager = statManager;
        this.xpManager = xpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Puntos disponibles: " + xpManager.getUnspentPoints(player), NamedTextColor.GOLD));
            player.sendMessage(Component.text("Usa /stats add <stat> para invertir", NamedTextColor.YELLOW));
            return true;
        }
        if (args[0].equalsIgnoreCase("add") && args.length >= 2) {
            try {
                StatType type = StatType.valueOf(args[1].toUpperCase());
                statManager.addStatPoint(player, type);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(Component.text("Estadística inválida", NamedTextColor.RED));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            statManager.resetPoints(player);
            return true;
        }
        player.sendMessage(Component.text("Uso: /stats [add <stat>|reset]", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public String getCommandName() {
        return "stats";
    }
}
