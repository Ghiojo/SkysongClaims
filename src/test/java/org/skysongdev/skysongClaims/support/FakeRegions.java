package org.skysongdev.skysongClaims.support;

import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;

import org.skysongdev.skysongClaims.worldguard.RegionAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** WorldGuard regions for tests: only the ones added exist. */
public final class FakeRegions implements RegionGateway {
    private final Set<ClaimRegion> regions = new HashSet<>();
    private final Map<ClaimRegion, RegionAccess> applied = new HashMap<>();
    private final Set<String> unknownFlags = new HashSet<>();

    public FakeRegions add(String world, String... ids) {
        for (String id : ids) {
            regions.add(new ClaimRegion(world, id));
        }
        return this;
    }

    public void remove(ClaimRegion region) {
        regions.remove(region);
    }

    /** Makes WorldGuard not know that flag, so {@link #apply} skips it. */
    public FakeRegions unknownFlag(String flag) {
        unknownFlags.add(flag);
        return this;
    }

    /** @return what was last applied to the region, or {@code null} */
    public RegionAccess applied(String world, String id) {
        return applied.get(new ClaimRegion(world, id));
    }

    @Override
    public List<String> apply(ClaimRegion region, RegionAccess access) {
        if (!regions.contains(region)) {
            return List.of();
        }
        applied.put(region, access);
        return access.openUse().stream().filter(unknownFlags::contains).toList();
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
