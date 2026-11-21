package org.skysongdev.skysongClaims.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.Groups;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Objects;

/** {@link Groups}, read from the Claims in memory. */
final class GroupsApi implements Groups {
    private final ApiLookup lookup;

    GroupsApi(ApiLookup lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    @Override
    public @NotNull List<ClaimGroup> of(@NotNull String claimId) {
        return lookup.claim(claimId).groups();
    }

    @Override
    public boolean canBuild(@NotNull CharacterId character, @NotNull String claimId) {
        Objects.requireNonNull(character, "character");
        Claim claim = lookup.claim(claimId);
        return claim.isOwnedBy(character) || claim.groups().stream()
                .anyMatch(group -> group.has(GroupPermission.BUILD) && group.members().contains(character));
    }
}
