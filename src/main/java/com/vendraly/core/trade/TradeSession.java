package com.vendraly.core.trade;

import com.vendraly.core.Main;
import com.vendraly.core.economy.CashManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Representa una sesión de tradeo activa entre dos jugadores.
 * CRITICAL FIX: Se almacena la referencia del Inventory (tradeInventory) para prevenir NullPointerException.
 * Se mejoró el manejo asíncrono del dinero en completeTrade.
 */
public class TradeSession {

    private final Main plugin;
    private final Player player1; // El jugador que abrió el GUI
    private final Player player2;
    private final CashManager cashManager;
    private final TradeGUI gui;

    // CRITICAL FIX: Almacenar la referencia al inventario una vez creado para evitar NullPointerException.
    private Inventory tradeInventory;

    private double moneyOffer1 = 0.0;
    private double moneyOffer2 = 0.0;
    private boolean isReady1 = false;
    private boolean isReady2 = false;

    // Almacenar el máximo de efectivo disponible al iniciar la sesión para el GUI.
    private final double maxCash1;
    private final double maxCash2;

    public TradeSession(Main plugin, Player p1, Player p2) {
        this.plugin = plugin;
        this.player1 = p1;
        this.player2 = p2;
        this.cashManager = plugin.getCashManager();
        this.gui = new TradeGUI(this);

        // Bloqueo y obtención del saldo actual de forma sincrónica al inicio de la sesión
        // El join() al inicio es un riesgo, pero necesario para inicializar el tradeo con saldos correctos.
        this.maxCash1 = getPlayerCashSafely(p1);
        this.maxCash2 = getPlayerCashSafely(p2);
    }

    /**
     * Método auxiliar para obtener el efectivo del jugador de forma segura.
     */
    private double getPlayerCashSafely(Player player) {
        try {
            // Se usa join() para esperar el resultado antes de continuar, asumiendo que el CashManager lo permite.
            return cashManager.getBalance(player.getUniqueId()).join();
        } catch (Exception e) {
            plugin.getLogger().severe("Error CRITICO al obtener el saldo de efectivo de " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Error crítico al obtener tu saldo. Tradeo cancelado.");
            return 0.0;
        }
    }


    /**
     * Crea el inventario y lo abre para ambos jugadores.
     */
    public void openGUI() {
        // 1. Crear y almacenar la referencia del inventario
        this.tradeInventory = gui.createInventory();

        // 2. Abrir para ambos
        player1.openInventory(this.tradeInventory);
        player2.openInventory(this.tradeInventory);
    }

    /**
     * Cierra el inventario para ambos jugadores.
     */
    public void closeGUI() {
        if (tradeInventory != null) {
            // Asegurarse de que ambos jugadores vean el inventario cerrado
            player1.closeInventory();
            player2.closeInventory();
        }
    }

    public List<Player> getPlayers() {
        return Arrays.asList(player1, player2);
    }

    public Player getOtherPlayer(Player player) {
        return player.equals(player1) ? player2 : player1;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    /**
     * Obtiene el efectivo máximo que el jugador puede ofrecer (caché al inicio).
     */
    public double getPlayerMaxCash(Player player) {
        return player.equals(player1) ? maxCash1 : maxCash2;
    }

    public double getMoneyOffer(Player player) {
        return player.equals(player1) ? moneyOffer1 : moneyOffer2;
    }

    public boolean isReady(Player player) {
        return player.equals(player1) ? isReady1 : isReady2;
    }

    /**
     * Intenta establecer la cantidad de efectivo que un jugador ofrece.
     */
    public boolean setMoneyOffer(Player player, double amount) {
        if (tradeInventory == null) return false; // Fail fast si el inventario no existe

        amount = Math.round(amount * 100.0) / 100.0;

        if (amount < 0) amount = 0;

        // CRITICAL FIX: Usar el saldo máximo cacheado para evitar consultas ASÍNCRONAS/BLOQUEANTES constantes
        double currentMaxCash = getPlayerMaxCash(player);

        // Verificar el límite de efectivo
        if (amount > currentMaxCash) {
            player.sendMessage(ChatColor.RED + String.format("No tienes $%.2f en efectivo para ofrecer. Tienes $%.2f.", amount, currentMaxCash));
            return false;
        }

        // Resetear el estado si se cambia la oferta de dinero
        isReady1 = false;
        isReady2 = false;

        if (player.equals(player1)) {
            moneyOffer1 = amount;
        } else {
            moneyOffer2 = amount;
        }

        // Actualizar el GUI (usando la referencia almacenada, ¡SEGURO!)
        gui.updateMoneySlots(tradeInventory, player1, player2);
        gui.updateReadyStatus(tradeInventory, player1, player2);

        return true;
    }

    /**
     * Cambia el estado de "Listo" de un jugador.
     */
    public void toggleReady(Player player) {
        if (tradeInventory == null) return; // Fail fast si el inventario no existe

        if (player.equals(player1)) {
            isReady1 = !isReady1;
        } else {
            isReady2 = !isReady2;
        }

        // Actualizar el GUI (usando la referencia almacenada, ¡SEGURO!)
        gui.updateReadyStatus(tradeInventory, player1, player2);

        player.sendMessage(isReady(player) ?
                ChatColor.GREEN + "Estás LISTO. Esperando a " + getOtherPlayer(player).getName() + "." :
                ChatColor.YELLOW + "Has CANCELADO tu estado de listo."
        );

        // Verificar si ambos están listos
        if (isReady1 && isReady2) {
            completeTrade();
        }
    }

    /**
     * Revierte el estado de listo de ambos si un ítem o dinero se modificó.
     */
    public void resetReadyStatus(Player modifier) {
        if (tradeInventory == null) return; // Fail fast si el inventario no existe

        if (isReady1 || isReady2) {
            isReady1 = false;
            isReady2 = false;

            Player other = getOtherPlayer(modifier);

            modifier.sendMessage(ChatColor.RED + "El tradeo se ha reestablecido por movimiento. Vuelve a hacer clic en ACEPTAR.");
            other.sendMessage(ChatColor.RED + modifier.getName() + " ha modificado su oferta. Vuelve a hacer clic en ACEPTAR.");

            // Actualizar el GUI (usando la referencia almacenada, ¡SEGURO!)
            gui.updateReadyStatus(tradeInventory, player1, player2);
        }
    }

    /**
     * Finaliza la transacción, transfiere ítems y efectivo.
     */
    private void completeTrade() {
        if (tradeInventory == null) return; // Fail fast si el inventario no existe

        // 1. Transferir ítems
        // La transferencia de ítems se realiza primero y limpia el inventario de tradeo.
        transferItems(tradeInventory);

        // 2. Transferir efectivo (usando CompletableFuture para manejo asíncrono)
        CompletableFuture<Boolean> future1 = cashManager.modifyBalance(player1.getUniqueId(), -moneyOffer1); // Quitar P1 cash
        CompletableFuture<Boolean> future2 = cashManager.modifyBalance(player2.getUniqueId(), moneyOffer1);  // Dar P1 cash a P2
        CompletableFuture<Boolean> future3 = cashManager.modifyBalance(player2.getUniqueId(), -moneyOffer2); // Quitar P2 cash
        CompletableFuture<Boolean> future4 = cashManager.modifyBalance(player1.getUniqueId(), moneyOffer2);  // Dar P2 cash a P1

        // Esperar que todas las transferencias terminen y ejecutar la lógica final en el hilo principal
        CompletableFuture.allOf(future1, future2, future3, future4).thenAccept(v -> {
            boolean success = future1.join() && future2.join() && future3.join() && future4.join();

            // Ejecutar la lógica de finalización en el hilo principal de Bukkit (necesario para mensajes y Scoreboard)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    player1.sendMessage(ChatColor.DARK_GREEN + "Tradeo con " + player2.getName() + " completado exitosamente.");
                    player2.sendMessage(ChatColor.DARK_GREEN + "Tradeo con " + player1.getName() + " completado exitosamente.");
                } else {
                    player1.sendMessage(ChatColor.RED + "Error en la transferencia de efectivo. Contacta a un admin.");
                    player2.sendMessage(ChatColor.RED + "Error en la transferencia de efectivo. Contacta a un admin.");
                }

                // 3. Finalizar la sesión
                plugin.getTradeManager().endTrade(this);

                // 4. Actualizar Scoreboard (Asumiendo que estos métodos son seguros de llamar)
                // Se deben actualizar en el hilo principal.
                plugin.getScoreboardManager().updatePlayerBoard(player1);
                plugin.getScoreboardManager().updatePlayerBoard(player2);
            });
        }).exceptionally(ex -> {
            // Manejo de errores si alguna promesa falla
            plugin.getLogger().severe("Error durante la transferencia de efectivo en el tradeo: " + ex.getMessage());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player1.sendMessage(ChatColor.RED + "Error crítico en la transferencia de efectivo. Contacta a un admin.");
                player2.sendMessage(ChatColor.RED + "Error crítico en la transferencia de efectivo. Contacta a un admin.");
                plugin.getTradeManager().endTrade(this); // Asegurar el cierre
            });
            return null;
        });
    }

    /**
     * Realiza la transferencia de ítems y los devuelve a los inventarios reales.
     */
    private void transferItems(Inventory tradeInv) {

        // I. Recoger los ítems del lado 1 (serán para el Jugador 2)
        List<ItemStack> itemsFrom1 = TradeGUI.PLAYER_SLOTS_1.stream()
                .map(tradeInv::getItem)
                .filter(item -> item != null && item.getType() != Material.AIR)
                .collect(Collectors.toList());

        // II. Recoger los ítems del lado 2 (serán para el Jugador 1)
        List<ItemStack> itemsFrom2 = TradeGUI.PLAYER_SLOTS_2.stream()
                .map(tradeInv::getItem)
                .filter(item -> item != null && item.getType() != Material.AIR)
                .collect(Collectors.toList());

        // III. Limpiar los slots del inventario de tradeo
        TradeGUI.PLAYER_SLOTS_1.forEach(slot -> tradeInv.setItem(slot, null));
        TradeGUI.PLAYER_SLOTS_2.forEach(slot -> tradeInv.setItem(slot, null));

        // IV. Distribución a los jugadores, se ejecuta en el hilo principal.
        distributeItems(player2, itemsFrom1);
        distributeItems(player1, itemsFrom2);
    }

    /**
     * Método de utilidad para añadir ítems al inventario de un jugador o dropearlos.
     */
    private void distributeItems(Player player, List<ItemStack> items) {
        // Aseguramos que la distribución se haga en el hilo principal
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            items.forEach(item -> player.getInventory().addItem(item).values().forEach(
                    overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow)));
        });
    }

    /**
     * Devuelve los ítems a los jugadores al cancelar.
     */
    public void returnItems(Inventory tradeInv) {
        if (tradeInv == null) return; // Fail fast si el inventario no existe

        // Recoger y devolver ítems del lado 1 al Jugador 1
        List<ItemStack> itemsFrom1 = TradeGUI.PLAYER_SLOTS_1.stream()
                .map(tradeInv::getItem)
                .filter(item -> item != null && item.getType() != Material.AIR)
                .collect(Collectors.toList());
        TradeGUI.PLAYER_SLOTS_1.forEach(slot -> tradeInv.setItem(slot, null));
        distributeItems(player1, itemsFrom1); // Devolver a P1

        // Recoger y devolver ítems del lado 2 al Jugador 2
        List<ItemStack> itemsFrom2 = TradeGUI.PLAYER_SLOTS_2.stream()
                .map(tradeInv::getItem)
                .filter(item -> item != null && item.getType() != Material.AIR)
                .collect(Collectors.toList());
        TradeGUI.PLAYER_SLOTS_2.forEach(slot -> tradeInv.setItem(slot, null));
        distributeItems(player2, itemsFrom2); // Devolver a P2
    }
}
