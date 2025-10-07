package com.vendraly.utils;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Utilidad para manipular la barra de vida Vanilla.
 * Oculta visualmente la barra de corazones sin requerir ProtocolLib,
 * usando el truco de establecer el MaxHealth en un valor que el cliente ignora temporalmente.
 *
 * NOTA: Este truco requiere que el MAX_HEALTH del jugador sea manipulado
 * y restaurado inmediatamente, por lo que es crucial pasarlo como argumento.
 */
public class NMSHealthUtil {

    // Valor no-estándar que 'rompe' la visualización de corazones en el cliente (1024.0)
    private static final double HIDE_VALUE = 1024.0;

    // Mínima diferencia para forzar un paquete de actualización de vida
    private static final double MIN_HEALTH_UPDATE = 0.0001;

    /**
     * Oculta la barra de salud Vanilla para el jugador.
     * Esta función debe llamarse al iniciar sesión y siempre que se reaparece.
     * * @param player El jugador al que se le ocultará la barra.
     * @param rpgMaxHealth El valor MAX_HEALTH real del jugador en el sistema RPG (ej: 1000.0).
     */
    public static void hideVanillaHealth(Player player, double rpgMaxHealth) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealthAttribute == null) {
            Bukkit.getLogger().severe("[VendralyCore] ERROR: El jugador " + player.getName() + " no tiene el atributo GENERIC_MAX_HEALTH.");
            return;
        }

        // 1. Guardamos el estado actual
        double actualHealth = player.getHealth();

        // Si la vida RPG Máxima pasada es 20, no hacemos el truco (solo si es un jugador Vanilla).
        if (rpgMaxHealth <= 20.0) {
            // Aseguramos que se muestre correctamente.
            maxHealthAttribute.setBaseValue(20.0);
            player.setHealth(Math.min(actualHealth, 20.0));
            return;
        }

        // --- INICIO DEL TRUCO DE OCULTAMIENTO ---

        // 2. Setear a un valor no-estándar (HIDE_VALUE) que rompe la visualización del HUD.
        maxHealthAttribute.setBaseValue(HIDE_VALUE);

        // 3. Forzar el envío del paquete de actualización de salud (CRÍTICO)
        // El cliente solo actualiza el HUD si detecta un cambio en la vida actual.
        boolean wasFullHealth = (actualHealth >= rpgMaxHealth);

        if (wasFullHealth) {
            // Si la vida está llena (ej: 1000/1000), la bajamos un poquito,
            // forzando al cliente a enviar el paquete de salud con el MAX_HEALTH 'roto' (1024).
            player.setHealth(rpgMaxHealth - MIN_HEALTH_UPDATE);
        }

        // 4. Restaurar el VALOR RPG ORIGINAL (ej: 1000.0) INMEDIATAMENTE.
        maxHealthAttribute.setBaseValue(rpgMaxHealth);

        // 5. Restaurar la vida actual original.
        if (wasFullHealth) {
            player.setHealth(rpgMaxHealth);
        } else {
            // Si la vida no estaba llena, solo aseguramos que el valor se mantenga.
            player.setHealth(actualHealth);
        }

        // --- FIN DEL TRUCO DE OCULTAMIENTO ---
    }
}
