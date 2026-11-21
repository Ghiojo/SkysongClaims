package org.skysongdev.skysongClaims.worldguard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.api.event.ProfileUpdateEvent;

/** Gives a player the rights of the character they play now, when they switch profile or join. */
public final class SyncListener implements Listener {
    private final WorldGuardSync sync;

    public SyncListener(WorldGuardSync sync) {
        this.sync = sync;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileSwitch(ProfileUpdateEvent event) {
        sync.syncPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sync.syncPlayer(event.getPlayer().getUniqueId());
    }
}
