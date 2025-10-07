package com.vendraly.core;

import com.vendraly.core.rpg.combat.DamageEngine;
import com.vendraly.core.rpg.item.ItemLoreUpdater;
import com.vendraly.core.rpg.item.ItemMetadataKeys;
import com.vendraly.core.rpg.listener.ItemLoreUpdaterListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

// Comandos Base
import com.vendraly.commands.LoginCommand;
import com.vendraly.commands.RegisterCommand;
import com.vendraly.commands.TestCommand;
// Comandos de Economía
import com.vendraly.commands.EconomyCommand;
import com.vendraly.commands.PayCommand;
import com.vendraly.commands.TradeCommand;
// Comandos de Roles y Moderación
import com.vendraly.commands.SetRoleCommand;
import com.vendraly.commands.VendralyBanCommand;
import com.vendraly.commands.VendralyUnbanCommand;
import com.vendraly.commands.RPGZoneCommand;
// Comando administrativo
import com.vendraly.commands.RpgExpCommand;

// Gestores base
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.auth.AuthManager;
import com.vendraly.core.economy.EconomyManager;
import com.vendraly.core.economy.CashManager;
import com.vendraly.core.trade.TradeManager;

// RPG
import com.vendraly.core.rpg.StatManager;
import com.vendraly.core.rpg.XPManager;
import com.vendraly.core.rpg.ParryManager;
import com.vendraly.core.rpg.MonsterListener;
import com.vendraly.core.listeners.StatListener;
import com.vendraly.core.rpg.WorldDifficultyManager;
import com.vendraly.core.rpg.ZoneSpawner;

// Habilidades
import com.vendraly.core.rpg.ability.AbilityManager;
import com.vendraly.core.rpg.ability.TailoringListener;
import com.vendraly.core.rpg.ability.ApothecaryListener;
import com.vendraly.core.rpg.ability.BlacksmithingListener;

// Combate direccional
import com.vendraly.core.rpg.combat.DirectionalAttackManager;
import com.vendraly.core.listeners.CameraChangeListener;
import com.vendraly.core.listeners.DirectionalAttackListener;

// Base listeners
import com.vendraly.core.listeners.PlayerListener;
import com.vendraly.core.listeners.TradeListener;
import com.vendraly.core.listeners.VillagerTradeListener;
import com.vendraly.core.listeners.ChatListener;
import com.vendraly.core.listeners.LootRestrictionListener;
import com.vendraly.core.scoreboard.ScoreboardManager;
import com.vendraly.listeners.PlayerJoinListener;
import com.vendraly.core.listeners.ItemRequirementListener;

// Estamina
import com.vendraly.core.rpg.stats.StaminaBossBarManager;
import com.vendraly.core.rpg.stats.StaminaRegenTask;

public class Main extends JavaPlugin {

    private static Main instance;

    // Gestores base
    private UserDataManager userDataManager;
    private AuthManager authManager;
    private PlayerListener playerListener;
    private EconomyManager economyManager;
    private ScoreboardManager scoreboardManager;
    private CashManager cashManager;
    private TradeManager tradeManager;

    // RPG
    private StatManager statManager;
    private WorldDifficultyManager difficultyManager;
    private ZoneSpawner zoneSpawner;
    private AbilityManager abilityManager;
    private XPManager xpManager;
    private MonsterListener monsterListener; // 🔧 Expuesto para ZoneSpawner

    // Combate direccional
    private DirectionalAttackManager directionalAttackManager;

    // Items
    private ItemMetadataKeys itemMetadataKeys;
    private ItemLoreUpdater itemLoreUpdater;

    // Estamina
    private StaminaBossBarManager staminaBossBarManager;
    private StaminaRegenTask staminaRegenTask;

    @Override
    public void onEnable() {
        instance = this;
        setupConfiguration();

        getLogger().info("[VendralyCore] Iniciando plugin con sistema YAML...");

        // ----------------------------------------------------------------------
        // FASE 1: GESTORES DE DATOS Y CLAVES
        // ----------------------------------------------------------------------
        this.userDataManager = new UserDataManager(this);
        this.authManager = new AuthManager(this);
        this.economyManager = new EconomyManager(this);
        this.cashManager = new CashManager(this);
        this.tradeManager = new TradeManager(this);
        this.itemMetadataKeys = new ItemMetadataKeys(this);

        // ----------------------------------------------------------------------
        // FASE 2: GESTORES RPG
        // ----------------------------------------------------------------------
        this.statManager = new StatManager(this); // ✅ solo este
        DamageEngine.init(this.statManager);      // ✅ inicializar con el mismo
        getLogger().info("DamageEngine inicializado con StatManager.");

        this.xpManager = new XPManager(this, this.statManager);
        this.difficultyManager = new WorldDifficultyManager(this);
        this.zoneSpawner = new ZoneSpawner(this);
        this.abilityManager = new AbilityManager(this);
        this.directionalAttackManager = new DirectionalAttackManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.itemLoreUpdater = new ItemLoreUpdater(this, this.itemMetadataKeys);

        // Estamina (bossbar + task)
        this.staminaBossBarManager = new StaminaBossBarManager(this, new HashMap<>());
        this.staminaRegenTask = new StaminaRegenTask(this, this.staminaBossBarManager, 20);
        this.staminaRegenTask.runTaskTimer(this, 0L, 20L); // arranca ya y repite cada 20 ticks

        // ----------------------------------------------------------------------
        // FASE 3: REGISTRO EVENTOS Y COMANDOS
        // ----------------------------------------------------------------------
        registerCommands();
        registerListeners();

        // ----------------------------------------------------------------------
        // FASE 4: INICIO DE SCHEDULERS
        // ----------------------------------------------------------------------
        if (this.statManager != null) this.statManager.startRegenScheduler();
        if (this.zoneSpawner != null) this.zoneSpawner.startSpawnerTask();
        if (this.scoreboardManager != null) this.scoreboardManager.startUpdateTask();

        getLogger().info("[VendralyCore] Plugin activado correctamente con sistema YAML.");
    }


    @Override
    public void onDisable() {
        // Detener tasks
        if (this.scoreboardManager != null) this.scoreboardManager.stopUpdateTask();
        if (this.directionalAttackManager != null) this.directionalAttackManager.stop();
        if (this.statManager != null) this.statManager.stopRegenScheduler();
        if (this.zoneSpawner != null) this.zoneSpawner.stopSpawnerTask();
        if (this.staminaRegenTask != null) this.staminaRegenTask.cancel();

        // Limpiar BossBars
        if (this.staminaBossBarManager != null) {
            Bukkit.getOnlinePlayers().forEach(staminaBossBarManager::removeStaminaBossBar);
        }

        if (this.difficultyManager != null) this.difficultyManager.saveConfiguration();
        if (this.userDataManager != null) this.userDataManager.saveAll();

        getLogger().info("[VendralyCore] Plugin desactivado.");
    }

    // --- GETTERS ---
    public static Main getInstance() { return instance; }
    public UserDataManager getUserDataManager() { return userDataManager; }
    public AuthManager getAuthManager() { return authManager; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public CashManager getCashManager() { return cashManager; }
    public TradeManager getTradeManager() { return tradeManager; }
    public StatManager getStatManager() { return statManager; }
    public WorldDifficultyManager getDifficultyManager() { return difficultyManager; }
    public ZoneSpawner getZoneSpawner() { return zoneSpawner; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public XPManager getXPManager() { return xpManager; }
    public ItemMetadataKeys getItemMetadataKeys() { return itemMetadataKeys; }
    public ItemLoreUpdater getItemLoreUpdater() { return itemLoreUpdater; }
    public DirectionalAttackManager getDirectionalAttackManager() { return directionalAttackManager; }
    public StaminaBossBarManager getStaminaBossBarManager() { return staminaBossBarManager; }
    public MonsterListener getMonsterListener() { return monsterListener; } // ✅ Necesario para ZoneSpawner

    // --- REGISTROS ---
    private void registerCommands() {
        PluginCommand cmd;

        cmd = getCommand("register");
        if (cmd != null) cmd.setExecutor(new RegisterCommand(this));

        cmd = getCommand("login");
        if (cmd != null) cmd.setExecutor(new LoginCommand(this));

        cmd = getCommand("testcore");
        if (cmd != null) cmd.setExecutor(new TestCommand());

        cmd = getCommand("eco");
        if (cmd != null) cmd.setExecutor(new EconomyCommand(this));

        cmd = getCommand("pay");
        if (cmd != null) cmd.setExecutor(new PayCommand(this));

        cmd = getCommand("trade");
        if (cmd != null) cmd.setExecutor(new TradeCommand(this, this.tradeManager));

        cmd = getCommand("setrole");
        if (cmd != null) cmd.setExecutor(new SetRoleCommand(this));

        cmd = getCommand("vban");
        if (cmd != null) cmd.setExecutor(new VendralyBanCommand(this));

        cmd = getCommand("vunban"); // 🔧 FIX: no uses 'banCommand' aquí
        if (cmd != null) cmd.setExecutor(new VendralyUnbanCommand(this));

        cmd = getCommand("stats");
        if (cmd != null) cmd.setExecutor(this.statManager);

        cmd = getCommand("atributos");
        if (cmd != null) cmd.setExecutor(this.statManager);

        cmd = getCommand("rpgzone");
        if (cmd != null) cmd.setExecutor(new RPGZoneCommand(this));

        cmd = getCommand("rgpexp");
        if (cmd != null) cmd.setExecutor(new RpgExpCommand(this));
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        // Listeners que dependen de StatManager
        pm.registerEvents(this.statManager, this);

        // Combate direccional
        pm.registerEvents(new CameraChangeListener(this), this);
        pm.registerEvents(new DirectionalAttackListener(this), this);

        // Player join (sincroniza stats/menus, etc.)
        pm.registerEvents(new PlayerJoinListener(this.statManager), this);

        // PlayerListener (auth/movimiento/chat bloqueado)
        this.playerListener = new PlayerListener(this);
        pm.registerEvents(this.playerListener, this);

        // Comercio/aldeanos/loot/chat
        pm.registerEvents(new TradeListener(this), this);
        pm.registerEvents(new VillagerTradeListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new LootRestrictionListener(), this);

        // Items (lore auto y requisitos)
        pm.registerEvents(new ItemLoreUpdaterListener(this, this.itemLoreUpdater), this);
        pm.registerEvents(new ItemRequirementListener(this), this);

        // Parry/StatListener/Mobs
        pm.registerEvents(new ParryManager(this), this);
        pm.registerEvents(new StatListener(this, this.statManager), this);

        // MonsterListener → guardamos referencia para ZoneSpawner
        this.monsterListener = new MonsterListener(this);
        pm.registerEvents(this.monsterListener, this);

        // Habilidades
        pm.registerEvents(new TailoringListener(this, this.itemMetadataKeys), this);
        pm.registerEvents(new ApothecaryListener(this), this);
        pm.registerEvents(new BlacksmithingListener(this), this);
    }

    private void setupConfiguration() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                saveDefaultConfig();
                getLogger().info("Se ha generado el archivo config.yml por defecto.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "No se pudo crear el archivo config.yml", e);
            }
        }
        File userDataFolder = new File(getDataFolder(), "userData");
        if (!userDataFolder.exists()) userDataFolder.mkdirs();
    }
}
