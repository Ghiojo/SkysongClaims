package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;

/**
 * A Claim was put on sale or taken off: see {@link ClaimInfo#forSaleBy()}.
 */
public final class ClaimSaleChangeEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ClaimSaleChangeEvent(@NotNull ClaimInfo claim, @NotNull ChangeCause cause) {
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
