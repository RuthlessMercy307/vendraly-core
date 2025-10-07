package com.vendraly.commands;

import com.vendraly.core.Main;
import com.vendraly.core.rpg.SpawnZone;
import com.vendraly.core.rpg.WorldDifficultyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Necesario para .filter(Objects::nonNull)
import java.util.UUID;
import java.util.stream.Collectors;

public class RPGZoneCommand implements CommandExecutor {

    private final Main plugin;
    private final WorldDifficultyManager difficultyManager;

    // Almacenamiento temporal de las selecciones de zona por jugador
    private final Map<UUID, Location> selectionsP1 = new HashMap<>();
    private final Map<UUID, Location> selectionsP2 = new HashMap<>();

    // VALORES POR DEFECTO PARA EL NUEVO CONSTRUCTOR DE SPAWNZONER
    private static final int DEFAULT_MAX_MOBS = 10;
    private static final int DEFAULT_SPAWN_DELAY_TICKS = 200; // 10 segundos (20 ticks/segundo)


    public RPGZoneCommand(Main plugin) {
        this.plugin = plugin;
        this.difficultyManager = plugin.getDifficultyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por un jugador.").color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("vendraly.admin.rpgzone")) {
            player.sendMessage(Component.text("No tienes permiso para usar este comando.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select" -> handleSelect(player, args);
            case "create" -> handleCreate(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }

        return true;
    }

    // ===============================================
    // MANEJADORES DE SUBCOMANDOS
    // ===============================================

    private void handleSelect(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Component.text("Uso correcto: /rpgzone select <1|2>").color(NamedTextColor.YELLOW));
            return;
        }

        // Usamos la Location del bloque donde está el jugador para definir las esquinas del cubo.
        Location loc = player.getLocation().getBlock().getLocation();

        if (args[1].equals("1")) {
            selectionsP1.put(player.getUniqueId(), loc);
            player.sendMessage(Component.text("Posición 1 (P1) establecida en: ")
                    .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()).color(NamedTextColor.AQUA)));
        } else if (args[1].equals("2")) {
            selectionsP2.put(player.getUniqueId(), loc);
            player.sendMessage(Component.text("Posición 2 (P2) establecida en: ")
                    .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()).color(NamedTextColor.AQUA)));
        } else {
            player.sendMessage(Component.text("Uso correcto: /rpgzone select <1|2>").color(NamedTextColor.YELLOW));
        }
    }

    private void handleCreate(Player player, String[] args) {
        // /rpgzone create <nombre> <minLvl> <maxLvl> <Mob1,Mob2,...>
        if (args.length != 5) {
            player.sendMessage(Component.text("Uso correcto: /rpgzone create <nombre> <minLvl> <maxLvl> <Mob1,Mob2,...>").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Ej: /rpgzone create Bosque_Inicial 1 5 ZOMBIE,SKELETON,SPIDER").color(NamedTextColor.YELLOW));
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!selectionsP1.containsKey(uuid) || !selectionsP2.containsKey(uuid)) {
            player.sendMessage(Component.text("Debes seleccionar P1 y P2 primero con /rpgzone select <1|2>.").color(NamedTextColor.RED));
            return;
        }

        String name = args[1];
        int minLvl, maxLvl;
        String mobListString = args[4];

        try {
            minLvl = Integer.parseInt(args[2]);
            maxLvl = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Los niveles deben ser números enteros válidos.").color(NamedTextColor.RED));
            return;
        }

        if (minLvl < 1 || maxLvl < minLvl) {
            player.sendMessage(Component.text("Niveles inválidos. minLvl debe ser >= 1 y maxLvl >= minLvl.").color(NamedTextColor.RED));
            return;
        }

        // VALIDAR TIPOS DE MOB (SOLUCIÓN DEL ERROR DE TIPOS)
        List<EntityType> mobTypes = Arrays.stream(mobListString.toUpperCase().split(","))
                .map(String::trim)
                // Usamos flatMap para convertir el String en un Optional<EntityType> o stream vacío
                .flatMap(mobName -> {
                    try {
                        EntityType type = EntityType.valueOf(mobName);
                        // Excluimos jugadores y otros tipos que no queremos spawnear
                        if (type.isAlive() && type != EntityType.PLAYER) {
                            return java.util.stream.Stream.of(type);
                        } else {
                            return java.util.stream.Stream.empty();
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("Advertencia: El tipo de mob '").color(NamedTextColor.YELLOW)
                                .append(Component.text(mobName).color(NamedTextColor.RED))
                                .append(Component.text("' no es válido o no existe.").color(NamedTextColor.YELLOW)));
                        return java.util.stream.Stream.empty();
                    }
                })
                // Forzamos el tipo final, aunque el error original se suele resolver con el flatMap o el cast
                .collect(Collectors.toList());


        if (mobTypes.isEmpty()) {
            player.sendMessage(Component.text("Error: Debes especificar al menos un tipo de mob válido para spawnear.").color(NamedTextColor.RED));
            return;
        }

        Location p1 = selectionsP1.get(uuid);
        Location p2 = selectionsP2.get(uuid);

        if (!p1.getWorld().equals(p2.getWorld())) {
            player.sendMessage(Component.text("P1 y P2 deben estar en el mismo mundo.").color(NamedTextColor.RED));
            return;
        }

        // Crear y guardar la SpawnZone (usa el constructor actualizado con los 8 argumentos)
        // ESTO SOLUCIONA EL ERROR "Expected 8 arguments but found 7"
        SpawnZone newZone = new SpawnZone(
                name,
                p1.getWorld(),
                minLvl,
                maxLvl,
                p1,
                p2,
                mobTypes,
                DEFAULT_MAX_MOBS, // Argumento 7
                DEFAULT_SPAWN_DELAY_TICKS // Argumento 8
        );
        difficultyManager.addZone(newZone);

        // Limpiar selecciones después de crear
        selectionsP1.remove(uuid);
        selectionsP2.remove(uuid);

        player.sendMessage(Component.text("¡Zona RPG '").color(NamedTextColor.GREEN)
                .append(Component.text(name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text("' creada con éxito!").color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Nivel: " + minLvl + "-" + maxLvl + " | Mobs: " + mobTypes.size()).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Configuración por defecto: Max Mobs=" + DEFAULT_MAX_MOBS + ", Delay=" + (DEFAULT_SPAWN_DELAY_TICKS / 20) + "s.").color(NamedTextColor.GRAY));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Component.text("Uso correcto: /rpgzone remove <nombre>").color(NamedTextColor.YELLOW));
            return;
        }

        String name = args[1];
        if (difficultyManager.removeZone(name)) {
            player.sendMessage(Component.text("Zona RPG '").color(NamedTextColor.GREEN)
                    .append(Component.text(name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text("' eliminada con éxito.").color(NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Error: No se encontró una zona con el nombre '").color(NamedTextColor.RED)
                    .append(Component.text(name).color(NamedTextColor.YELLOW)).append(Component.text("'.").color(NamedTextColor.RED)));
        }
    }

    private void handleList(Player player) {
        player.sendMessage(Component.text("--- Zonas RPG de Dificultad ---").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));

        List<SpawnZone> zones = difficultyManager.getSafeZones();

        if (zones.isEmpty()) {
            player.sendMessage(Component.text("No hay zonas de spawn definidas.").color(NamedTextColor.GRAY));
            return;
        }

        for (SpawnZone zone : zones) {
            String mobList = zone.getMobTypes().stream()
                    .map(Enum::name)
                    .limit(3)
                    .collect(Collectors.joining(", "));

            String mobCount = (zone.getMobTypes().size() > 3) ? " (+" + (zone.getMobTypes().size() - 3) + ")" : "";

            player.sendMessage(Component.text("» ").color(NamedTextColor.YELLOW)
                    .append(Component.text(zone.getName()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text(" [Lv. " + zone.getMinLevel() + "-" + zone.getMaxLevel() + "]"))
                    .append(Component.text(" | Mobs: " + mobList + mobCount)));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- Ayuda /RPGZone ---").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("/rpgzone select 1").color(NamedTextColor.YELLOW).append(Component.text(" - Establece P1.")));
        player.sendMessage(Component.text("/rpgzone select 2").color(NamedTextColor.YELLOW).append(Component.text(" - Establece P2.")));
        player.sendMessage(Component.text("/rpgzone create <nombre> <min> <max> <Mobs>").color(NamedTextColor.YELLOW).append(Component.text(" - Crea la zona. (Mobs separados por coma)")));
        player.sendMessage(Component.text("/rpgzone remove <nombre>").color(NamedTextColor.YELLOW).append(Component.text(" - Elimina la zona.")));
        player.sendMessage(Component.text("/rpgzone list").color(NamedTextColor.YELLOW).append(Component.text(" - Muestra las zonas definidas.")));
    }
}