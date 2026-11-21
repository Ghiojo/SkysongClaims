package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;

/**
 * Staff deleted a Claim. {@link #getClaim()} is the Claim as it was just before.
 */
public final class ClaimDeleteEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ClaimDeleteEvent(@NotNull ClaimInfo claim, @NotNull ChangeCause cause) {
        super(claim, cause);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
