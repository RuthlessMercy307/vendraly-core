package com.vendraly.core.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Define la estructura visual del inventario de tradeo (Chest GUI de 54 slots).
 * Se corrigió el diseño para una apariencia más limpia y organizada.
 */
public class TradeGUI {

    private final TradeSession session;
    private static final String TITLE = ChatColor.DARK_BLUE + ChatColor.BOLD.toString() + "Tradeo Seguro";
    private static final int INVENTORY_SIZE = 54;

    // Slots de ítems para el Jugador 1 (Columnas 0, 1, 2, 3) - Lado Izquierdo
    public static final List<Integer> PLAYER_SLOTS_1 = IntStream.range(0, 36)
            .filter(i -> i % 9 < 4)
            .boxed().collect(Collectors.toList());

    // Slots de ítems para el Jugador 2 (Columnas 5, 6, 7, 8) - Lado Derecho
    public static final List<Integer> PLAYER_SLOTS_2 = IntStream.range(0, 36)
            .filter(i -> i % 9 > 4)
            .boxed().collect(Collectors.toList());

    // Slots de separación (Columna 4) - Separador vertical de las ofertas
    public static final List<Integer> SEPARATOR_SLOTS = IntStream.range(0, 36)
            .filter(i -> i % 9 == 4)
            .boxed().collect(Collectors.toList());

    // Slots de Control (Fila inferior, 36 a 53)
    public static final int P1_OFFER_SLOT = 41; // MUESTRA LA CANTIDAD DE DINERO DE P1
    public static final int P2_OFFER_SLOT = 49; // MUESTRA LA CANTIDAD DE DINERO DE P2

    public static final int P1_STATUS_SLOT = 43; // Slot de Aceptar/Pendiente
    public static final int P2_STATUS_SLOT = 51; // Slot de Aceptar/Pendiente

    // Slots de Control de Dinero para P1
    public static final int P1_INCREASE_10 = 37;
    public static final int P1_INCREASE_1 = 38;
    public static final int P1_DECREASE_10 = 46;
    public static final int P1_DECREASE_1 = 47;

    // Slots de Control de Dinero para P2
    public static final int P2_INCREASE_10 = 44;
    public static final int P2_INCREASE_1 = 45;
    public static final int P2_DECREASE_10 = 52;
    public static final int P2_DECREASE_1 = 53;

    // Slots vacíos para rellenar (Índices que quedan libres en la fila 36-53)
    // El slot 40 es el botón de CANCELAR.
    public static final List<Integer> CONTROL_SLOTS = Arrays.asList(
            36, 39, 42, 48, 50
    );

    public TradeGUI(TradeSession session) {
        this.session = session;
    }

    /**
     * Crea y devuelve el inventario para la sesión de tradeo.
     * @return El inventario de 54 slots.
     */
    public Inventory createInventory() {
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE);
        fillInterface(inv);
        return inv;
    }

    /**
     * Llena el inventario con los ítems de fondo y control.
     */
    public void fillInterface(Inventory inv) {
        // Ítem de relleno (separador y fondo de control)
        ItemStack fillerSeparator = createFillerItem(Material.IRON_BARS, ChatColor.GRAY + "Separador de Ofertas");
        // Filler oscuro para el área de control, más estético que las barras de hierro.
        ItemStack fillerControl = createFillerItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // 1. Separador vertical (IRON_BARS)
        SEPARATOR_SLOTS.forEach(slot -> inv.setItem(slot, fillerSeparator));

        // 2. Slots de fondo en la fila de control (BLACK_STAINED_GLASS_PANE)
        CONTROL_SLOTS.forEach(slot -> inv.setItem(slot, fillerControl));

        // 3. Botón de Cancelar (Slot central de la fila inferior, 40)
        inv.setItem(40, createItem(Material.RED_WOOL, ChatColor.RED + ChatColor.BOLD.toString() + "CANCELAR TRADE",
                Arrays.asList(ChatColor.GRAY + "Haz clic para finalizar el tradeo y devolver los ítems.")));

        // 4. Ítems de Control de Dinero
        inv.setItem(P1_INCREASE_10, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "+ $10.00"));
        inv.setItem(P1_INCREASE_1, createItem(Material.EMERALD, ChatColor.GREEN + "+ $1.00"));
        inv.setItem(P1_DECREASE_10, createItem(Material.REDSTONE_BLOCK, ChatColor.RED + "- $10.00"));
        inv.setItem(P1_DECREASE_1, createItem(Material.REDSTONE, ChatColor.RED + "- $1.00"));

        inv.setItem(P2_INCREASE_10, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "+ $10.00"));
        inv.setItem(P2_INCREASE_1, createItem(Material.EMERALD, ChatColor.GREEN + "+ $1.00"));
        inv.setItem(P2_DECREASE_10, createItem(Material.REDSTONE_BLOCK, ChatColor.RED + "- $10.00"));
        inv.setItem(P2_DECREASE_1, createItem(Material.REDSTONE, ChatColor.RED + "- $1.00"));

        // 5. Slots de dinero y estado inicial
        Player p1 = session.getPlayer1();
        Player p2 = session.getPlayer2();
        updateReadyStatus(inv, p1, p2);
        updateMoneySlots(inv, p1, p2);
    }

    /**
     * Actualiza el estado de Aceptar/Pendiente (lana verde/roja) en el inventario.
     */
    public void updateReadyStatus(Inventory inv, Player p1, Player p2) {
        // P1 Status
        boolean p1Ready = session.isReady(p1);
        inv.setItem(P1_STATUS_SLOT, createStatusItem(p1, p1Ready));

        // P2 Status
        boolean p2Ready = session.isReady(p2);
        inv.setItem(P2_STATUS_SLOT, createStatusItem(p2, p2Ready));
    }

    /**
     * Actualiza la cantidad de dinero ofrecida en el slot central.
     */
    public void updateMoneySlots(Inventory inv, Player p1, Player p2) {
        // Slot de dinero del Jugador 1
        inv.setItem(P1_OFFER_SLOT, createMoneyItem(p1, session.getMoneyOffer(p1)));

        // Slot de dinero del Jugador 2
        inv.setItem(P2_OFFER_SLOT, createMoneyItem(p2, session.getMoneyOffer(p2)));
    }

    // --- Métodos de Utilidad para Ítems ---

    private ItemStack createFillerItem(Material material, String name) {
        // Ítem sin Lore, solo para relleno y decoración.
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }


    private ItemStack createMoneyItem(Player player, double amount) {
        String title = ChatColor.YELLOW + "Dinero Ofrecido (" + player.getName() + ")";
        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Monto: " + ChatColor.GOLD + String.format("$%.2f", amount),
                " ",
                ChatColor.AQUA + "Usa los botones de Esmeralda/Redstone para ajustar el monto.",
                ChatColor.DARK_GRAY + "Máximo que puedes ofrecer: " + ChatColor.GOLD + String.format("$%.2f", session.getPlayerMaxCash(player))
        );
        return createItem(Material.GOLD_BLOCK, title, lore);
    }

    private ItemStack createStatusItem(Player player, boolean isReady) {
        Material material = isReady ? Material.LIME_WOOL : Material.RED_WOOL;
        String title = isReady ? ChatColor.GREEN + ChatColor.BOLD.toString() + "¡LISTO PARA ACEPTAR!" : ChatColor.RED + ChatColor.BOLD.toString() + "PENDIENTE";

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Haz clic para cambiar tu estado.",
                ChatColor.GRAY + "Tu oferta debe estar finalizada para aceptar."
        );

        return createItem(material, title, lore);
    }

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
