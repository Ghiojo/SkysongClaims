package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;

/**
 * A character stopped owning a Claim. {@link #getCause()} says why: {@link ChangeCause#LEAVE} when it
 * left, {@link ChangeCause#COMMAND} when staff removed it, {@link ChangeCause#API} when a plugin did,
 * {@link ChangeCause#RECONCILE} when its SkysongStats profile is gone.
 */
public final class ClaimOwnerRemoveEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;

    public ClaimOwnerRemoveEvent(@NotNull ClaimInfo claim, @NotNull CharacterId character, @NotNull ChangeCause cause) {
        super(claim, cause);
        this.character = Objects.requireNonNull(character, "character");
    }

    /** @return the former owner */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
