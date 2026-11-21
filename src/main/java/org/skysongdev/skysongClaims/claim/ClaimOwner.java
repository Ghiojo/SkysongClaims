package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.Objects;

/**
 * One owner of a Claim. Owners are equal: none of them can remove another.
 *
 * @param character the SkysongStats character that owns the Claim
 * @param since     when it became an owner
 * @param cause     how it became an owner
 */
public record ClaimOwner(CharacterId character, Instant since, OwnerCause cause) {
    public ClaimOwner {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(since, "since");
        Objects.requireNonNull(cause, "cause");
    }
}
