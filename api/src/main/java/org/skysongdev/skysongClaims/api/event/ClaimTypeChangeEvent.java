package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;

import java.util.Objects;

/**
 * Staff changed a Claim's type.
 */
public final class ClaimTypeChangeEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String previousTypeId;

    public ClaimTypeChangeEvent(@NotNull ClaimInfo claim, @NotNull String previousTypeId, @NotNull ChangeCause cause) {
        super(claim, cause);
        this.previousTypeId = Objects.requireNonNull(previousTypeId, "previousTypeId");
    }

    /** @return the id of the type it had before */
    public @NotNull String getPreviousTypeId() {
        return previousTypeId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
