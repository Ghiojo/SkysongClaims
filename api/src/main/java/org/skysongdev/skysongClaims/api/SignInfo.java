package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A sign that shows a Claim.
 *
 * @param claimId the Claim it shows
 * @param world   the world's name
 * @param x       block x
 * @param y       block y
 * @param z       block z
 */
public record SignInfo(@NotNull String claimId, @NotNull String world, int x, int y, int z) {
    public SignInfo {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(world, "world");
    }
}
