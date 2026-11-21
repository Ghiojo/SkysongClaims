package org.skysongdev.skysongClaims.worldguard;

import org.bukkit.Location;
import org.skysongdev.skysongClaims.claim.ClaimRegion;

import java.util.List;
import java.util.Optional;

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
     * player or permission group typed in by hand, sets its open-use flags to {@code allow},
     * and sets the owners' flags.
     *
     * @return the flags that were skipped because WorldGuard doesn't know them, an open-use flag
     *         isn't an allow/deny flag, or an owner's value no longer parses; empty when
     *         everything was applied or the region doesn't exist
     */
    List<String> apply(ClaimRegion region, RegionAccess access);

    /** Removes a flag from the region, e.g. one its owners cleared. */
    void clearFlag(ClaimRegion region, String flag);

    /**
     * @return why WorldGuard won't take the value for the flag, empty when it would; the reason
     *         is WorldGuard's own message
     */
    Optional<String> checkFlag(String flag, String value);
}
