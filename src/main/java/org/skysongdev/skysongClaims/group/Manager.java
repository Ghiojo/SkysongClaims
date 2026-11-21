package org.skysongdev.skysongClaims.group;

import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;
import java.util.Optional;

/** Who is managing a Claim's groups or flags: one of its owners, or staff, who may manage any Claim. */
public final class Manager {
    private static final Manager STAFF = new Manager(Optional.empty());

    private final Optional<CharacterId> owner;

    private Manager(Optional<CharacterId> owner) {
        this.owner = owner;
    }

    public static Manager owner(CharacterId character) {
        return new Manager(Optional.of(Objects.requireNonNull(character, "character")));
    }

    public static Manager staff() {
        return STAFF;
    }

    /** @throws RefusedException {@code ownership.not-owner} when an owner manager doesn't own the Claim */
    public void check(Claim claim) {
        if (owner.isPresent() && !claim.isOwnedBy(owner.get())) {
            throw RefusedException.of("ownership.not-owner", "claim", claim.id());
        }
    }
}
