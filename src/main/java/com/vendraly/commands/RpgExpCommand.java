package com.vendraly.commands;

import com.vendraly.core.rpg.stats.XPManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para otorgar XP RPG.
 */
public class RpgExpCommand implements CommandExecutorHolder {

    private final XPManager xpManager;

    public RpgExpCommand(XPManager xpManager) {
        this.xpManager = xpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vendraly.rpg.exp")) {
            sender.sendMessage(Component.text("Sin permisos", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /rgpexp <jugador> <cantidad>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED));
            return true;
        }
        try {
            long amount = Long.parseLong(args[1]);
            xpManager.addExperience(target, amount);
            sender.sendMessage(Component.text("Experiencia otorgada", NamedTextColor.GREEN));
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("Cantidad inválida", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public String getCommandName() {
        return "rgpexp";
    }
}
