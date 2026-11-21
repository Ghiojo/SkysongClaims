package org.skysongdev.skysongClaims.worldguard;

import org.skysongdev.skysongClaims.claim.ClaimRegion;

import java.util.List;

/** What SkysongClaims needs from WorldGuard's regions. Implemented on WorldGuard, faked in tests. */
public interface RegionGateway {
    /** @return whether the region exists in WorldGuard, in a loaded world */
    boolean exists(ClaimRegion region);

    /** @return the ids of every region in the world, for tab completion; empty when the world isn't loaded */
    List<String> regionIds(String world);
}
