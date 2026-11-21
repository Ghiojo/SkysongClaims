package org.skysongdev.skysongClaims.api;

import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Optional;

/** Reads Claims. Main thread only. */
@ApiStatus.NonExtendable
public interface Claims {
    /** @return every Claim, by id */
    @NotNull List<ClaimInfo> all();

    @NotNull Optional<ClaimInfo> find(@NotNull String claimId);

    /** @return the Claim at the location: the one with the highest-priority WorldGuard region there */
    @NotNull Optional<ClaimInfo> at(@NotNull Location location);

    /** @return the Claim a WorldGuard region belongs to */
    @NotNull Optional<ClaimInfo> ofRegion(@NotNull String world, @NotNull String regionId);

    /** @return the Claims the character owns */
    @NotNull List<ClaimInfo> ownedBy(@NotNull CharacterId character);
}
