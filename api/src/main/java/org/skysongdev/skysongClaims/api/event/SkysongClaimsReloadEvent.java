package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Staff reloaded SkysongClaims' config, so Claim types (and their {@code extra} settings) may
 * have changed. Fired on the main thread after the reload.
 */
public final class SkysongClaimsReloadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
