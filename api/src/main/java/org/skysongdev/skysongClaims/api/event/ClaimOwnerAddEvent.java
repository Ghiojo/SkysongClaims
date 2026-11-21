package org.skysongdev.skysongClaims.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;

/**
 * A character became an owner of a Claim. {@link #getCause()} says how: {@link ChangeCause#FREE_CLAIM}
 * when it took a Free Claim slot, {@link ChangeCause#COMMAND} when it accepted an invite or staff
 * added it, {@link ChangeCause#API} when a plugin did (e.g. a sale).
 */
public final class ClaimOwnerAddEvent extends ClaimEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;

    public ClaimOwnerAddEvent(@NotNull ClaimInfo claim, @NotNull CharacterId character, @NotNull ChangeCause cause) {
        super(claim, cause);
        this.character = Objects.requireNonNull(character, "character");
    }

    /** @return the new owner */
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
