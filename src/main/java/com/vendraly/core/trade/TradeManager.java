package com.vendraly.core.trade;

import com.vendraly.core.Main;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestiona las solicitudes de tradeo pendientes, su expiración y las sesiones activas.
 */
public class TradeManager {

    private final Main plugin;
    // Mapeo: Jugador que recibe la solicitud -> Jugador que envió la solicitud
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    // Mapeo para gestionar la cancelación automática: Jugador que recibe -> Tarea de cancelación
    private final Map<UUID, BukkitTask> cancellationTasks = new HashMap<>();

    // Mapeo: Jugador -> Sesión de Tradeo activa
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    // Tiempo de expiración en ticks (20 ticks = 1 segundo)
    private static final int EXPIRATION_TICKS = 10 * 20; // 10 segundos

    public TradeManager(Main plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("§a[VendralyCore] TradeManager inicializado.");
    }

    /* -------------------------------------
       ENUMS REQUERIDOS POR TRADE COMMAND
       ------------------------------------- */

    /**
     * Enum para resultados de solicitud de trade (/trade <jugador>).
     */
    public enum RequestResult {
        OK, // Solicitud enviada con éxito
        ALREADY_SENT, // Ya existe una solicitud pendiente del mismo jugador
        TARGET_HAS_PENDING_REQUEST, // El objetivo ya tiene una solicitud de trade de otro
        IN_TRADE // Uno de los jugadores ya está en un trade activo
    }

    /**
     * Enum para resultados de aceptación de trade (/trade accept <jugador>).
     */
    public enum AcceptResult {
        OK, // Trade aceptado y GUI abriéndose
        NO_PENDING_REQUEST, // No hay solicitud pendiente de ese jugador
        REQUESTER_OFFLINE, // El jugador que envió la solicitud se desconectó
        ALREADY_IN_TRADE // El jugador que acepta ya está en un trade
    }

    /* -------------------------------------
       MÉTODOS DE FACHADA (API para TradeCommand)
       ------------------------------------- */

    /**
     * Devuelve una instancia de Player a partir de un nombre, usando Optional.
     */
    public Optional<Player> getTargetPlayer(String targetName) {
        // Usa getPlayerExact para evitar problemas con nombres parciales
        return Optional.ofNullable(Bukkit.getPlayerExact(targetName));
    }

    /**
     * Envía una solicitud de tradeo del requester al target.
     * FACHADA para TradeCommand.
     */
    public RequestResult requestTrade(Player requester, Player target) {
        // Bloqueo: Verificar si ya están en un trade activo
        if (isInTrade(requester) || isInTrade(target)) {
            // El comando ya maneja el mensaje, solo devolvemos el resultado.
            return RequestResult.IN_TRADE;
        }

        UUID targetId = target.getUniqueId();
        UUID requesterId = requester.getUniqueId();

        // Bloqueo 1: Si ya ha enviado solicitud al mismo objetivo
        if (pendingRequests.containsKey(targetId) && pendingRequests.get(targetId).equals(requesterId)) {
            return RequestResult.ALREADY_SENT;
        }

        // Bloqueo 2: Si el objetivo tiene una solicitud pendiente de CUALQUIERA (Tu lógica original)
        // El comando lo interpreta como TARGET_HAS_PENDING_REQUEST
        if (pendingRequests.containsKey(targetId)) {
            // Devolvemos este error y la solicitud se cancela/reemplaza en la lógica interna (sendRequest)
            return RequestResult.TARGET_HAS_PENDING_REQUEST;
        }

        // Llama a la lógica interna de tu TradeManager
        sendRequest(requester, target);
        return RequestResult.OK;
    }

    /**
     * Gestiona la aceptación de un trade.
     * FACHADA para TradeCommand.
     */
    public AcceptResult acceptTrade(Player receiver, String requesterName) {
        UUID receiverId = receiver.getUniqueId();

        // Verificar si el receiver ya está en un trade activo.
        if (isInTrade(receiver)) {
            return AcceptResult.ALREADY_IN_TRADE;
        }

        // 1. Verificar solicitud pendiente del requester
        if (!pendingRequests.containsKey(receiverId)) {
            return AcceptResult.NO_PENDING_REQUEST;
        }

        UUID requesterId = pendingRequests.get(receiverId);
        Player requester = Bukkit.getPlayer(requesterId);

        // 2. Verificar si el requester existe y es el correcto
        if (requester == null || !requester.isOnline()) {
            // El jugador se desconectó
            return AcceptResult.REQUESTER_OFFLINE;
        }

        // 3. Verificar que el nombre del comando coincide con el UUID almacenado
        if (!requester.getName().equalsIgnoreCase(requesterName)) {
            // Esto sucede si /trade accept X es llamado, pero la solicitud pendiente era de Y.
            return AcceptResult.NO_PENDING_REQUEST;
        }

        // Llama a la lógica interna de tu TradeManager
        acceptRequest(receiver, requester);
        return AcceptResult.OK;
    }

    /**
     * Cancela una solicitud pendiente o un trade activo.
     * FACHADA para TradeCommand.
     */
    public boolean cancelTrade(Player player) {
        // 1. Intentar cancelar solicitud (enviada o recibida)
        boolean requestCancelled = cancelPendingRequest(player);

        // 2. Intentar cancelar trade activo
        TradeSession session = activeTrades.get(player.getUniqueId());
        if (session != null) {
            endTrade(session);
            player.sendMessage(ChatColor.RED + "Tu sesión de tradeo ha sido cancelada.");
            return true;
        }

        return requestCancelled;
    }

    /**
     * Devuelve el UUID del último jugador que envió una solicitud al jugador dado.
     * Usado por TradeCommand para enviar un mensaje de éxito al solicitante.
     */
    public UUID getLastRequesterId(UUID receiverId) {
        return pendingRequests.get(receiverId);
    }

    /* -------------------------------------
       LÓGICA INTERNA (Tu código original, ligeramente modificado)
       ------------------------------------- */

    /**
     * LÓGICA INTERNA: Envía una solicitud de tradeo del sender al target.
     * Usado internamente por requestTrade.
     */
    private boolean sendRequest(Player sender, Player target) {
        // Los checks de 'isInTrade' ya se hicieron en requestTrade()

        UUID targetId = target.getUniqueId();
        UUID senderId = sender.getUniqueId();

        // 1. Cancelar cualquier solicitud anterior al target y su tarea de expiración.
        // Esto maneja la lógica de TARGET_HAS_PENDING_REQUEST al reemplazarla.
        cancelExistingRequest(target);

        // 2. Registrar la nueva solicitud
        pendingRequests.put(targetId, senderId);

        // 3. Iniciar el temporizador de expiración
        startExpirationTimer(target, sender);

        // El TradeCommand se encarga de los mensajes de éxito
        return true;
    }

    /**
     * LÓGICA INTERNA: Acepta una solicitud de tradeo.
     * Usado internamente por acceptTrade.
     */
    private boolean acceptRequest(Player accepter, Player requester) {
        UUID accepterId = accepter.getUniqueId();

        // Los checks de solicitud pendiente ya se hicieron en acceptTrade()

        // 1. Limpiar la solicitud y cancelar la tarea de expiración
        pendingRequests.remove(accepterId);
        BukkitTask task = cancellationTasks.remove(accepterId);
        if (task != null) {
            task.cancel();
        }

        // 2. Crear y registrar la sesión de tradeo
        // ASUMO que tienes una clase TradeSession que maneja la GUI.
        TradeSession session = new TradeSession(plugin, accepter, requester);
        activeTrades.put(requester.getUniqueId(), session);
        activeTrades.put(accepter.getUniqueId(), session);

        session.openGUI(); // Abrir la interfaz
        return true;
    }

    private void startExpirationTimer(Player target, Player sender) {
        // Programar una tarea que se ejecute una vez después de 10 segundos
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            // Verificar si la solicitud sigue siendo válida (es decir, no fue aceptada)
            if (pendingRequests.containsKey(target.getUniqueId()) && pendingRequests.get(target.getUniqueId()).equals(sender.getUniqueId())) {

                // Remover la solicitud expirada
                pendingRequests.remove(target.getUniqueId());
                cancellationTasks.remove(target.getUniqueId());

                // Notificar a los jugadores
                if (target.isOnline()) {
                    target.sendMessage(ChatColor.RED + "La solicitud de tradeo de " + sender.getName() + " ha expirado.");
                }
                if (sender.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "Tu solicitud de tradeo a " + target.getName() + " ha expirado.");
                }
            }
        }, EXPIRATION_TICKS);

        cancellationTasks.put(target.getUniqueId(), task);
    }

    private void cancelExistingRequest(Player target) {
        // Cancelar la tarea de expiración anterior
        BukkitTask task = cancellationTasks.remove(target.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        // Informar al remitente anterior que su solicitud fue reemplazada
        UUID oldSenderId = pendingRequests.remove(target.getUniqueId());
        if (oldSenderId != null) {
            Player oldSender = plugin.getServer().getPlayer(oldSenderId);
            if (oldSender != null && oldSender.isOnline()) {
                oldSender.sendMessage(ChatColor.RED + "Tu solicitud de tradeo a " + target.getName() + " fue reemplazada y cancelada.");
            }
        }
    }

    /**
     * Intenta cancelar una solicitud pendiente que el jugador ha enviado o recibido.
     * @param player El jugador.
     * @return true si alguna solicitud fue cancelada.
     */
    private boolean cancelPendingRequest(Player player) {
        UUID playerId = player.getUniqueId();
        boolean cancelled = false;

        // 1. Buscar si tiene una solicitud PENDIENTE (es el receptor)
        if (pendingRequests.containsKey(playerId)) {
            cancelExistingRequest(player); // Cancela la tarea y notifica al remitente.
            player.sendMessage(ChatColor.GREEN + "Tu solicitud de tradeo recibida ha sido cancelada.");
            cancelled = true;
        }

        // 2. Buscar si ha ENVIADO alguna solicitud
        // Recorrer todas las solicitudes donde 'playerId' sea el remitente
        UUID receiverId = pendingRequests.entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (receiverId != null) {
            Player receiver = Bukkit.getPlayer(receiverId);
            if (receiver != null) {
                // Si encontramos el receptor, usamos cancelExistingRequest para limpiar.
                cancelExistingRequest(receiver);
                receiver.sendMessage(ChatColor.RED + player.getName() + " canceló su solicitud de tradeo.");
            } else {
                // El receptor está offline, simplemente limpiamos la solicitud
                pendingRequests.remove(receiverId);
                BukkitTask task = cancellationTasks.remove(receiverId);
                if (task != null) task.cancel();
            }
            player.sendMessage(ChatColor.GREEN + "Tu solicitud de tradeo enviada ha sido cancelada.");
            cancelled = true;
        }

        return cancelled;
    }


    /**
     * Obtiene la sesión de tradeo activa de un jugador.
     */
    public TradeSession getSession(Player player) {
        return activeTrades.get(player.getUniqueId());
    }

    /**
     * Verifica si un jugador está en una sesión de tradeo activa.
     */
    public boolean isInTrade(Player player) {
        return activeTrades.containsKey(player.getUniqueId());
    }

    /**
     * Cierra y finaliza una sesión de tradeo.
     */
    public void endTrade(TradeSession session) {
        // Usamos el Bukkit Scheduler para cerrar inventarios en el siguiente tick
        plugin.getServer().getScheduler().runTask(plugin, session::closeGUI);

        // Remover de los mapas
        session.getPlayers().forEach(p -> activeTrades.remove(p.getUniqueId()));
    }
}