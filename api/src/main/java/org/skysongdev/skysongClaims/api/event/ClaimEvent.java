package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;

import java.util.Objects;

/**
 * A change to a Claim, fired on the main thread right after the change is made and saved. It
 * can't be cancelled: to stop a change, check before making it.
 */
public abstract class ClaimEvent extends Event {
    private final ClaimInfo claim;
    private final ChangeCause cause;

    protected ClaimEvent(@NotNull ClaimInfo claim, @NotNull ChangeCause cause) {
        this.claim = Objects.requireNonNull(claim, "claim");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** @return the Claim after the change; for a delete, as it was just before */
    public @NotNull ClaimInfo getClaim() {
        return claim;
    }

    public @NotNull ChangeCause getCause() {
        return cause;
    }
}
