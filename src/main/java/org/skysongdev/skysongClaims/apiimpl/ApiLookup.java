package org.skysongdev.skysongClaims.apiimpl;

import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * What every API part does first: checks the thread, then finds the Claim or type it was given.
 * An unknown id is the calling plugin's mistake, so it throws instead of answering a refusal.
 */
final class ApiLookup {
    private final MainThread mainThread;
    private final ClaimState claims;
    private final Supplier<ClaimTypeRegistry> types;

    ApiLookup(MainThread mainThread, ClaimState claims, Supplier<ClaimTypeRegistry> types) {
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.claims = Objects.requireNonNull(claims, "claims");
        this.types = Objects.requireNonNull(types, "types");
    }

    /** @throws IllegalStateException off the main thread */
    void checkThread() {
        mainThread.check();
    }

    ClaimState claims() {
        return claims;
    }

    ClaimTypeRegistry types() {
        return types.get();
    }

    /** Checks the thread, then finds the Claim. @throws IllegalArgumentException when no Claim has the id */
    Claim claim(String claimId) {
        checkThread();
        Objects.requireNonNull(claimId, "claimId");
        return claims.find(claimId).orElseThrow(() -> new IllegalArgumentException("No claim has the id " + claimId));
    }

    /** Checks the thread, then finds the type. @throws IllegalArgumentException when types.yml has no such type */
    ClaimType type(String typeId) {
        checkThread();
        Objects.requireNonNull(typeId, "typeId");
        return types.get().find(typeId).orElseThrow(() -> new IllegalArgumentException("types.yml has no type " + typeId));
    }
}
