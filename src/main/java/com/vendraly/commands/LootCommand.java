package com.vendraly.commands;

import com.vendraly.core.rpg.loot.LootTableManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Comando para listar tablas de loot.
 */
public class LootCommand implements CommandExecutorHolder {

    private final LootTableManager lootTableManager;

    public LootCommand(LootTableManager lootTableManager) {
        this.lootTableManager = lootTableManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Component.text("Tablas de loot registradas:", NamedTextColor.GOLD));
        lootTableManager.getTables().forEach((id, entries) -> sender.sendMessage(Component.text("- " + id + " (" + entries.size() + " entradas)", NamedTextColor.GREEN)));
        return true;
    }

    @Override
    public String getCommandName() {
        return "loot";
    }
}
