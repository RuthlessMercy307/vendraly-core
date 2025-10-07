package com.vendraly.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor; // Para usar colores en el chat

/**
 * Clase que maneja la lógica del comando /testcore.
 */
public class TestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verificamos si quien ejecuta el comando es un jugador
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Enviamos un mensaje de éxito al jugador.
            // Usamos ChatColor.translateAlternateColorCodes('&', ...) para permitir códigos de color '&'
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a¡VendralyCore está funcionando! &b" + player.getName() + "&a, bienvenido a Purpur 1.21.8."));

        } else {
            // Si el remitente es la consola
            sender.sendMessage("¡VendralyCore está funcionando! Este comando solo es para prueba en consola.");
        }

        // Siempre devuelve 'true' si el comando se ha manejado correctamente.
        return true;
    }
}
