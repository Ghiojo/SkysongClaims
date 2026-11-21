package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.Claims;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimLocator;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link Claims} over the Claims in memory. */
final class ClaimsApi implements Claims {
    private final ApiLookup lookup;
    private final ClaimLocator locator;

    ClaimsApi(ApiLookup lookup, ClaimLocator locator) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.locator = Objects.requireNonNull(locator, "locator");
    }

    @Override
    public @NotNull List<ClaimInfo> all() {
        lookup.checkThread();
        return lookup.claims().all().stream().map(Claim::info).toList();
    }

    @Override
    public @NotNull Optional<ClaimInfo> find(@NotNull String claimId) {
        lookup.checkThread();
        return lookup.claims().find(claimId).map(Claim::info);
    }

    @Override
    public @NotNull Optional<ClaimInfo> at(@NotNull Location location) {
        lookup.checkThread();
        return locator.at(Objects.requireNonNull(location, "location")).map(Claim::info);
    }

    @Override
    public @NotNull Optional<ClaimInfo> ofRegion(@NotNull String world, @NotNull String regionId) {
        lookup.checkThread();
        return lookup.claims().ofRegion(new ClaimRegion(world, regionId)).map(Claim::info);
    }

    @Override
    public @NotNull List<ClaimInfo> ownedBy(@NotNull CharacterId character) {
        lookup.checkThread();
        return lookup.claims().ownedBy(Objects.requireNonNull(character, "character")).stream().map(Claim::info).toList();
    }
}
