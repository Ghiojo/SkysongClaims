package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.Ownership;
import org.skysongdev.skysongClaims.api.OwnershipCheck;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Objects;

/**
 * {@link Ownership} over the {@link OwnershipService}. It checks first and answers why it
 * can't, so a plugin can refund instead of catching an exception.
 */
final class OwnershipApi implements Ownership {
    private final ApiLookup lookup;
    private final OwnershipService ownership;

    OwnershipApi(ApiLookup lookup, OwnershipService ownership) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
    }

    @Override
    public @NotNull List<ClaimOwner> owners(@NotNull String claimId) {
        return lookup.claim(claimId).owners();
    }

    @Override
    public boolean isOwner(@NotNull String claimId, @NotNull CharacterId character) {
        return lookup.claim(claimId).isOwnedBy(Objects.requireNonNull(character, "character"));
    }

    @Override
    public @NotNull OwnershipCheck canOwn(@NotNull CharacterId character, @NotNull String claimId) {
        return ownership.canOwn(Objects.requireNonNull(character, "character"), lookup.claim(claimId));
    }

    @Override
    public @NotNull OwnershipCheck addOwner(@NotNull Plugin plugin, @NotNull String claimId, @NotNull CharacterId character) {
        Objects.requireNonNull(plugin, "plugin");
        OwnershipCheck check = canOwn(character, claimId);
        if (check == OwnershipCheck.OK) {
            ownership.addOwner(claimId, character, OwnerCause.API, ChangeCause.API, false);
        }
        return check;
    }

    @Override
    public boolean removeOwner(@NotNull Plugin plugin, @NotNull String claimId, @NotNull CharacterId character) {
        Objects.requireNonNull(plugin, "plugin");
        if (!isOwner(claimId, character)) {
            return false;
        }
        ownership.removeOwner(claimId, character, ChangeCause.API);
        return true;
    }

    @Override
    public @NotNull OwnershipCheck transfer(@NotNull Plugin plugin, @NotNull String claimId, @NotNull CharacterId from,
                                            @NotNull CharacterId to) {
        Objects.requireNonNull(plugin, "plugin");
        Claim claim = lookup.claim(claimId);
        OwnershipCheck check = ownership.canTransfer(claim, Objects.requireNonNull(from, "from"), Objects.requireNonNull(to, "to"));
        if (check == OwnershipCheck.OK) {
            ownership.transfer(claimId, from, to, OwnerCause.API, ChangeCause.API);
        }
        return check;
    }

    @Override
    public void clearOwners(@NotNull Plugin plugin, @NotNull String claimId) {
        Objects.requireNonNull(plugin, "plugin");
        lookup.claim(claimId);
        ownership.clearOwners(claimId, ChangeCause.API);
    }
}
