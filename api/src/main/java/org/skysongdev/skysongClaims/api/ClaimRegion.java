package org.skysongdev.skysongClaims.api;

import java.util.Locale;
import java.util.Objects;

/**
 * One WorldGuard region of a Claim. WorldGuard region ids are case-insensitive and stored in
 * lower case, so the id is kept in lower case too.
 *
 * @param world    the world's name
 * @param regionId the WorldGuard region id
 */
public record ClaimRegion(String world, String regionId) {
    public ClaimRegion {
        Objects.requireNonNull(world, "world");
        regionId = Objects.requireNonNull(regionId, "regionId").toLowerCase(Locale.ROOT);
    }

    /** @return {@code world:region}, the way staff type it */
    @Override
    public String toString() {
        return world + ":" + regionId;
    }
}
