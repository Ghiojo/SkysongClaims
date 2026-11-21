package org.skysongdev.skysongClaims.claim;

import org.bukkit.Location;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;

import java.util.Objects;
import java.util.Optional;

/** Finds the Claim at a place: the one owning the highest-priority Claim region there. */
public final class ClaimLocator {
    private final ClaimState claims;
    private final RegionGateway regions;

    public ClaimLocator(ClaimState claims, RegionGateway regions) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.regions = Objects.requireNonNull(regions, "regions");
    }

    public Optional<Claim> at(Location location) {
        return regions.regionsAt(location).stream()
                .map(claims::ofRegion)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
