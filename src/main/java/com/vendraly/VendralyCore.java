package com.vendraly;

import com.vendraly.commands.*;
import com.vendraly.core.auth.AuthManager;
import com.vendraly.core.config.ConfigManager;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.economy.CashManager;
import com.vendraly.core.economy.EconomyManager;
import com.vendraly.core.jobs.JobManager;
import com.vendraly.core.clans.ClanManager;
import com.vendraly.core.protection.ProtectionManager;
import com.vendraly.core.roles.RoleManager;
import com.vendraly.core.rpg.ability.AbilityManager;
import com.vendraly.core.rpg.combat.CombatManager;
import com.vendraly.core.rpg.loot.LootTableManager;
import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stats.XPManager;
import com.vendraly.core.rpg.stamina.StaminaManager;
import com.vendraly.core.scoreboard.ScoreboardManager;
import com.vendraly.core.trade.TradeManager;
import com.vendraly.listeners.*;
import com.vendraly.core.rpg.listener.RPGPlayerListener;
import com.vendraly.core.tradingui.TradeGuiManager;
import com.vendraly.utils.TaskUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Punto de entrada del plugin VendralyCore. Inicializa todos los gestores
 * principales y coordina la carga de configuración, comandos, listeners y tareas
 * periódicas. Esta clase actúa como orquestador de los subsistemas de
 * autenticación, economía, clanes, RPG, comercio y protecciones.
 */
public final class VendralyCore extends JavaPlugin {

    private static VendralyCore instance;

    private Logger logger;
    private ConfigManager configManager;
    private UserDataManager userDataManager;
    private AuthManager authManager;
    private RoleManager roleManager;
    private EconomyManager economyManager;
    private CashManager cashManager;
    private TradeManager tradeManager;
    private TradeGuiManager tradeGuiManager;
    private JobManager jobManager;
    private ClanManager clanManager;
    private ProtectionManager protectionManager;
    private StatManager statManager;
    private XPManager xpManager;
    private StaminaManager staminaManager;
    private AbilityManager abilityManager;
    private CombatManager combatManager;
    private LootTableManager lootTableManager;
    private ScoreboardManager scoreboardManager;

    /**
     * Obtiene la instancia singleton del plugin.
     *
     * @return instancia principal
     */
    public static VendralyCore getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        this.logger = getLogger();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.userDataManager = new UserDataManager(this);
        this.roleManager = new RoleManager(this);
        this.authManager = new AuthManager(this, userDataManager, roleManager);
        this.economyManager = new EconomyManager(this, userDataManager);
        this.cashManager = new CashManager(this, userDataManager);
        this.tradeManager = new TradeManager(this, cashManager);
        this.tradeGuiManager = new TradeGuiManager();
        this.jobManager = new JobManager(this);
        this.clanManager = new ClanManager(configManager);
        this.protectionManager = new ProtectionManager(clanManager);
        this.xpManager = new XPManager(this, userDataManager);
        this.statManager = new StatManager(this, userDataManager, xpManager);
        this.staminaManager = new StaminaManager();
        this.abilityManager = new AbilityManager(statManager);
        this.combatManager = new CombatManager(this, statManager, abilityManager, staminaManager);
        this.lootTableManager = new LootTableManager(this);
        this.scoreboardManager = new ScoreboardManager(this, statManager, cashManager, economyManager);

        registerCommands();
        registerListeners();
        startSchedulers();

        logger.info("VendralyCore habilitado correctamente");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (tradeManager != null) {
            tradeManager.shutdown();
        }
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (staminaManager != null) {
            staminaManager.shutdown();
        }
        if (userDataManager != null) {
            userDataManager.saveAll();
        }
        logger.info("VendralyCore deshabilitado");
    }

    private void registerCommands() {
        register(new LoginCommand(authManager));
        register(new RegisterCommand(authManager));
        register(new EconomyCommand(economyManager, cashManager));
        register(new PayCommand(cashManager));
        register(new TradeCommand(tradeManager));
        register(new ClanCommand(clanManager));
        register(new JobCommand(jobManager));
        register(new RoleCommand(authManager));
        register(new StatsCommand(statManager, xpManager));
        register(new LootCommand(lootTableManager));
        register(new RpgExpCommand(xpManager));
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new PlayerConnectionListener(authManager, userDataManager, statManager, staminaManager, scoreboardManager), this);
        pluginManager.registerEvents(new AuthListener(authManager), this);
        pluginManager.registerEvents(new EconomyListener(cashManager, economyManager, protectionManager), this);
        pluginManager.registerEvents(new TradeListener(tradeManager, tradeGuiManager), this);
        pluginManager.registerEvents(new RPGPlayerListener(statManager, xpManager, jobManager, lootTableManager, abilityManager), this);
        pluginManager.registerEvents(new CombatListener(combatManager), this);
        pluginManager.registerEvents(new ProtectionListener(protectionManager), this);
        pluginManager.registerEvents(new ClanListener(clanManager), this);
        pluginManager.registerEvents(new JobListener(jobManager, xpManager), this);
        pluginManager.registerEvents(new LootChestListener(lootTableManager, protectionManager), this);
    }

    private void startSchedulers() {
        TaskUtil.runTimer(this, () -> scoreboardManager.updateAll(), 20L, 60L);
        TaskUtil.runTimer(this, () -> staminaManager.tick(), 20L, 10L);
        TaskUtil.runTimer(this, () -> abilityManager.tick(), 40L, 20L);
        TaskUtil.runTimer(this, () -> tradeManager.tick(), 20L, 20L);
    }

    private void register(CommandExecutorHolder holder) {
        PluginCommand command = getCommand(holder.getCommandName());
        if (command == null) {
            logger.warning("No se encontró el comando " + holder.getCommandName() + " en plugin.yml");
            return;
        }
        command.setExecutor(holder);
        if (holder instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public Logger getPluginLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public UserDataManager getUserDataManager() {
        return userDataManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public CashManager getCashManager() {
        return cashManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public TradeGuiManager getTradeGuiManager() {
        return tradeGuiManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public StatManager getStatManager() {
        return statManager;
    }

    public XPManager getXpManager() {
        return xpManager;
    }

    public StaminaManager getStaminaManager() {
        return staminaManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ConfigManager getConfigBridge() {
        return configManager;
    }

    public void broadcast(Component component) {
        Bukkit.broadcast(component);
    }
}
