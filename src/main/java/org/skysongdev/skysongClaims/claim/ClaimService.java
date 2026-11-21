package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * What staff do to Claims: make and delete them, change their regions, type, Free Claim
 * settings and name. Ownership has its own service. Every refusal is a
 * {@link RefusedException} with a {@code messages.yml} key.
 */
public final class ClaimService {
    private final ClaimStore store;
    private final Supplier<ClaimTypeRegistry> types;
    private final RegionGateway regions;

    /**
     * @param types   the types in use; a supplier, so a reload takes effect at once
     * @param regions checks that regions exist in WorldGuard
     */
    public ClaimService(ClaimStore store, Supplier<ClaimTypeRegistry> types, RegionGateway regions) {
        this.store = Objects.requireNonNull(store, "store");
        this.types = Objects.requireNonNull(types, "types");
        this.regions = Objects.requireNonNull(regions, "regions");
    }

    /**
     * Makes an unowned Claim. A Claim of a Free Claim type starts as a Free Claim.
     *
     * @throws RefusedException when the id is invalid or taken, the type is unknown, or a
     *                          region is missing from WorldGuard or in another Claim
     */
    public Claim create(String id, String typeId, List<ClaimRegion> claimRegions, ChangeCause cause) {
        if (!Claim.isValidId(id)) {
            throw RefusedException.of("claim.invalid-id", "claim", id);
        }
        if (store.state().find(id).isPresent()) {
            throw RefusedException.of("claim.id-taken", "claim", id);
        }
        ClaimType type = requireType(typeId);
        if (claimRegions.isEmpty()) {
            throw RefusedException.of("claim.no-regions", "claim", id);
        }
        claimRegions.forEach(region -> requireFreeRegion(region, id));
        Claim claim = Claim.create(id, type.id(), type.freeClaim(), type.maxOwners(), claimRegions);
        store.save(claim, cause);
        return claim;
    }

    /** @throws RefusedException {@code claim.unknown} when no Claim has that id */
    public Claim require(String id) {
        return store.require(id);
    }

    /** @throws RefusedException when there is no such Claim */
    public void delete(String id, ChangeCause cause) {
        store.delete(store.require(id).id(), cause);
    }

    /** @throws RefusedException when the region is missing from WorldGuard or in a Claim already */
    public Claim addRegion(String id, ClaimRegion region, ChangeCause cause) {
        Claim claim = store.require(id);
        requireFreeRegion(region, id);
        List<ClaimRegion> changed = new ArrayList<>(claim.regions());
        changed.add(region);
        return save(claim.withRegions(changed), cause);
    }

    /** @throws RefusedException when the region isn't in the Claim, or is its last one */
    public Claim removeRegion(String id, ClaimRegion region, ChangeCause cause) {
        Claim claim = store.require(id);
        if (!claim.regions().contains(region)) {
            throw RefusedException.of("claim.region-not-in-claim", "claim", id, "region", region.toString());
        }
        if (claim.regions().size() == 1) {
            throw RefusedException.of("claim.last-region", "claim", id);
        }
        List<ClaimRegion> changed = new ArrayList<>(claim.regions());
        changed.remove(region);
        return save(claim.withRegions(changed), cause);
    }

    /**
     * Changes the type. Owners keep the Claim even when it puts them over the new type's limit.
     *
     * @throws RefusedException when the type is unknown
     */
    public Claim setType(String id, String typeId, ChangeCause cause) {
        Claim claim = store.require(id);
        return save(claim.withType(requireType(typeId).id()), cause);
    }

    /**
     * Makes the Claim a Free Claim or not, and optionally changes its owner slots.
     *
     * @param maxOwners the new number of slots, or empty to keep the current one
     * @throws RefusedException when the slots are fewer than 1 or than its current owners
     */
    public Claim setFree(String id, boolean free, OptionalInt maxOwners, ChangeCause cause) {
        Claim claim = store.require(id);
        int slots = maxOwners.orElse(claim.maxOwners());
        requireSlots(claim, slots);
        return save(claim.withFree(free, slots), cause);
    }

    /**
     * Changes how many owners the Claim has room for.
     *
     * @throws RefusedException when it is fewer than 1 or than its current owners
     */
    public Claim setMaxOwners(String id, int maxOwners, ChangeCause cause) {
        Claim claim = store.require(id);
        requireSlots(claim, maxOwners);
        return save(claim.withFree(claim.free(), maxOwners), cause);
    }

    /** @throws RefusedException when the name is blank */
    public Claim rename(String id, String displayName, ChangeCause cause) {
        Claim claim = store.require(id);
        if (displayName.isBlank()) {
            throw RefusedException.of("claim.blank-name", "claim", id);
        }
        return save(claim.withDisplayName(displayName.strip()), cause);
    }

    /** @return the Claims whose type is no longer in {@code types.yml}, by id */
    public List<Claim> withUnknownType() {
        ClaimTypeRegistry registry = types.get();
        return store.state().all().stream().filter(claim -> registry.find(claim.typeId()).isEmpty()).toList();
    }


    private Claim save(Claim claim, ChangeCause cause) {
        store.save(claim, cause);
        return claim;
    }

    private ClaimType requireType(String typeId) {
        return types.get().find(typeId).orElseThrow(() -> RefusedException.of("claim.unknown-type", "type", typeId));
    }

    private void requireFreeRegion(ClaimRegion region, String claimId) {
        if (!regions.exists(region)) {
            throw RefusedException.of("claim.region-missing", "region", region.toString());
        }
        store.state().ofRegion(region).ifPresent(other -> {
            throw RefusedException.of("claim.region-taken", "region", region.toString(), "claim", other.id());
        });
    }

    private static void requireSlots(Claim claim, int slots) {
        if (slots < 1) {
            throw RefusedException.of("claim.max-owners-too-low", "claim", claim.id());
        }
        if (slots < claim.owners().size()) {
            throw RefusedException.of("claim.max-owners-below-owners", "claim", claim.id(),
                    "owners", Integer.toString(claim.owners().size()));
        }
    }
}
