package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.SignInfo;

import java.util.Objects;

/**
 * A sign started or stopped showing a Claim: staff linked or unlinked it, or its Claim or block
 * is gone. A newly linked sign's fourth line is blank, so a plugin that shows a price there sets
 * it again. Fired on the main thread.
 */
public final class ClaimSignChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final SignInfo sign;
    private final boolean linked;

    public ClaimSignChangeEvent(@NotNull SignInfo sign, boolean linked) {
        this.sign = Objects.requireNonNull(sign, "sign");
        this.linked = linked;
    }

    public @NotNull SignInfo getSign() {
        return sign;
    }

    /** @return true when the sign now shows the Claim, false when it stopped */
    public boolean isLinked() {
        return linked;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
