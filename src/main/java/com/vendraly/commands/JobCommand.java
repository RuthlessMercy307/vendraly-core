package com.vendraly.commands;

import com.vendraly.core.jobs.JobDefinition;
import com.vendraly.core.jobs.JobManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para listar oficios y su configuración.
 */
public class JobCommand implements CommandExecutorHolder {

    private final JobManager jobManager;

    public JobCommand(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }
        player.sendMessage(Component.text("Oficios disponibles:", NamedTextColor.GOLD));
        for (JobDefinition job : jobManager.getJobs()) {
            player.sendMessage(Component.text("- " + job.getDisplayName() + " | recompensa base " + job.getBaseReward(), NamedTextColor.AQUA));
        }
        return true;
    }

    @Override
    public String getCommandName() {
        return "jobs";
    }
}
