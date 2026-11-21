package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongClaims.api.event.ChangeCause;

import java.util.Objects;
import java.util.Optional;

/**
 * One change to a Claim: made ({@code before} empty), changed, or deleted ({@code after} empty).
 *
 * @param before the Claim before the change
 * @param after  the Claim after the change
 * @param cause  why it changed
 */
public record ClaimChange(Optional<Claim> before, Optional<Claim> after, ChangeCause cause) {
    public ClaimChange {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(cause, "cause");
        if (before.isEmpty() && after.isEmpty()) {
            throw new IllegalArgumentException("A change needs a Claim before or after it");
        }
    }

    /** @return the Claim's id */
    public String claimId() {
        return after.or(() -> before).orElseThrow().id();
    }

    public boolean isCreate() {
        return before.isEmpty();
    }

    public boolean isDelete() {
        return after.isEmpty();
    }
}
