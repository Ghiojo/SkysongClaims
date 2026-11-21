package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;

/**
 * Staff created a Claim. It starts unowned and not for sale.
 */
public final class ClaimCreateEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ClaimCreateEvent(@NotNull ClaimInfo claim, @NotNull ChangeCause cause) {
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
