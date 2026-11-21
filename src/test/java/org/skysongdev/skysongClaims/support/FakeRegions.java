package org.skysongdev.skysongClaims.support;

import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** WorldGuard regions for tests: only the ones added exist. */
public final class FakeRegions implements RegionGateway {
    private final Set<ClaimRegion> regions = new HashSet<>();

    public FakeRegions add(String world, String... ids) {
        for (String id : ids) {
            regions.add(new ClaimRegion(world, id));
        }
        return this;
    }

    public void remove(ClaimRegion region) {
        regions.remove(region);
    }

    @Override
    public boolean exists(ClaimRegion region) {
        return regions.contains(region);
    }

    @Override
    public List<String> regionIds(String world) {
        return regions.stream().filter(region -> region.world().equals(world)).map(ClaimRegion::regionId).sorted().toList();
    }
}
