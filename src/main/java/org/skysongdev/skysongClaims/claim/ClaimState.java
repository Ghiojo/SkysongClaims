package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Every Claim in memory, by id and by region. Only {@link ClaimService} and the other
 * services change it; everything else reads.
 */
public final class ClaimState {
    private final Map<String, Claim> byId = new TreeMap<>();
    private final Map<ClaimRegion, String> byRegion = new HashMap<>();

    /** Replaces everything with the Claims loaded at start-up. */
    public void load(Collection<Claim> claims) {
        byId.clear();
        byRegion.clear();
        claims.forEach(this::put);
    }

    public Optional<Claim> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** @return every Claim, by id */
    public List<Claim> all() {
        return List.copyOf(byId.values());
    }

    /** @return the Claim the region belongs to */
    public Optional<Claim> ofRegion(ClaimRegion region) {
        return Optional.ofNullable(byRegion.get(region)).map(byId::get);
    }

    /** @return the Claims the character owns, by id */
    public List<Claim> ownedBy(CharacterId character) {
        return byId.values().stream().filter(claim -> claim.isOwnedBy(character)).toList();
    }

    /** Adds the Claim, or replaces the one with its id. */
    void put(Claim claim) {
        remove(claim.id());
        byId.put(claim.id(), claim);
        claim.regions().forEach(region -> byRegion.put(region, claim.id()));
    }

    void remove(String id) {
        Claim old = byId.remove(id);
        if (old != null) {
            old.regions().forEach(byRegion::remove);
        }
    }
}
