package com.vendraly.commands;

import org.bukkit.command.CommandExecutor;

/**
 * Contrato común para comandos del plugin que permite registrar dinámicamente
 * sus manejadores a través de {@link com.vendraly.VendralyCore}.
 */
public interface CommandExecutorHolder extends CommandExecutor {

    /**
     * Nombre del comando tal cual está declarado en plugin.yml.
     *
     * @return identificador del comando
     */
    String getCommandName();
}
