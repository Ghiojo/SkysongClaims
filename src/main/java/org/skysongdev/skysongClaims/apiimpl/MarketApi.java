package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.Market;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link Market} over the {@link ClaimService}. */
final class MarketApi implements Market {
    private final ApiLookup lookup;
    private final ClaimService claims;

    MarketApi(ApiLookup lookup, ClaimService claims) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.claims = Objects.requireNonNull(claims, "claims");
    }

    @Override
    public boolean isForSale(@NotNull String claimId) {
        return lookup.claim(claimId).isForSale();
    }

    @Override
    public void setForSale(@NotNull Plugin plugin, @NotNull String claimId, boolean forSale) {
        Objects.requireNonNull(plugin, "plugin");
        lookup.claim(claimId);
        claims.setForSale(claimId, forSale ? Optional.of(plugin.getName()) : Optional.empty(), ChangeCause.API);
    }

    @Override
    public @NotNull List<ClaimInfo> forSale() {
        lookup.checkThread();
        return lookup.claims().all().stream().filter(Claim::isForSale).map(Claim::info).toList();
    }
}
