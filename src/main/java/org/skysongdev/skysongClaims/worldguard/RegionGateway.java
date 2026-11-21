package org.skysongdev.skysongClaims.worldguard;

import org.bukkit.Location;
import org.skysongdev.skysongClaims.claim.ClaimRegion;

import java.util.List;

/** What SkysongClaims needs from WorldGuard's regions. Implemented on WorldGuard, faked in tests. */
public interface RegionGateway {
    /** @return whether the region exists in WorldGuard, in a loaded world */
    boolean exists(ClaimRegion region);

    /** @return the ids of every region in the world, for tab completion; empty when the world isn't loaded */
    List<String> regionIds(String world);

    /** @return the regions at the location, highest WorldGuard priority first */
    List<ClaimRegion> regionsAt(Location location);

    /**
     * Makes the region's owners and members exactly those of {@code access}, removing any
     * player or permission group typed in by hand, and sets its open-use flags to {@code allow}.
     *
     * @return the flag names WorldGuard doesn't know or that aren't allow/deny flags, so they were skipped;
     *         empty when everything was applied or the region doesn't exist
     */
    List<String> apply(ClaimRegion region, RegionAccess access);
}
