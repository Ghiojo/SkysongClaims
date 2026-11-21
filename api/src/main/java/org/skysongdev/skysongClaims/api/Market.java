package org.skysongdev.skysongClaims.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Whether Claims are for sale. SkysongClaims only keeps yes or no and which plugin said so; the
 * price lives in that plugin (it can show it on the Claim's signs through {@link ClaimSigns}).
 * Main thread only; unknown Claim ids throw {@link IllegalArgumentException}.
 */
@ApiStatus.NonExtendable
public interface Market {
    boolean isForSale(@NotNull String claimId);

    /** Puts the Claim on sale or takes it off. Fires {@code ClaimSaleChangeEvent} when it changes. */
    void setForSale(@NotNull Plugin plugin, @NotNull String claimId, boolean forSale);

    /** @return every Claim for sale, by id */
    @NotNull List<ClaimInfo> forSale();
}
