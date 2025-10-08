package com.vendraly.listeners;

import com.vendraly.core.auth.AuthManager;
import com.vendraly.core.database.UserDataManager;
import com.vendraly.core.rpg.stats.StatManager;
import com.vendraly.core.rpg.stamina.StaminaManager;
import com.vendraly.core.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Gestiona la conexión de jugadores.
 */
public class PlayerConnectionListener implements Listener {

    private final AuthManager authManager;
    private final UserDataManager userDataManager;
    private final StatManager statManager;
    private final StaminaManager staminaManager;
    private final ScoreboardManager scoreboardManager;

    public PlayerConnectionListener(AuthManager authManager, UserDataManager userDataManager, StatManager statManager, StaminaManager staminaManager, ScoreboardManager scoreboardManager) {
        this.authManager = authManager;
        this.userDataManager = userDataManager;
        this.statManager = statManager;
        this.staminaManager = staminaManager;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        userDataManager.getOrCreate(player.getUniqueId(), player.getName());
        authManager.handleJoin(player);
        statManager.apply(player);
        staminaManager.initialize(player);
        scoreboardManager.show(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        authManager.handleQuit(player);
        staminaManager.remove(player.getUniqueId());
        scoreboardManager.remove(player.getUniqueId());
    }
}
