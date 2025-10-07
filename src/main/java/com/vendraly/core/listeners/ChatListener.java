package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.roles.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener que añade el prefijo de rol a los mensajes de chat.
 */
public class ChatListener implements Listener {

    private final Main plugin;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 1. Bloqueo de jugadores no autenticados
        if (plugin.getPlayerListener().isUnauthenticated(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Debes iniciar sesión para chatear.").color(NamedTextColor.RED));
            return;
        }

        // 2. Obtener rol del jugador
        Role role = plugin.getAuthManager().getPlayerRole(player);
        String prefix = String.valueOf(role.getFormattedPrefix()); // prefijo con color propio del rol

        // 3. Mensaje del jugador (según permisos de color)
        String rawMessage = event.getMessage();
        String finalMessage;

        if (role != Role.PLAYER && role != Role.MEDIA && role != Role.VIP) {
            // Roles con permiso de colores → traducimos códigos &a, &c, etc.
            finalMessage = ChatColor.translateAlternateColorCodes('&', rawMessage);
        } else {
            // Roles básicos → texto plano
            finalMessage = ChatColor.stripColor(rawMessage);
        }

        // 4. Definir formato final del chat
        // El formato debe ser tipo "%1$s: %2$s"
        // %1$s → nombre con prefijo, %2$s → mensaje del jugador
        String format = prefix + player.getName() + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + "%2$s";

        event.setFormat(format);
        event.setMessage(finalMessage);
    }
}
