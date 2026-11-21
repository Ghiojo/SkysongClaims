package org.skysongdev.skysongClaims.worldguard;

import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimChange;
import org.skysongdev.skysongClaims.claim.ClaimObserver;
import org.skysongdev.skysongClaims.claim.ClaimState;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Keeps WorldGuard in line with the Claims. SkysongClaims is the only writer of its regions'
 * owners, members and open-use flags: every Claim change, profile switch and join writes them
 * again, and so do start-up and reloads, which also put back flags someone changed by hand.
 */
public final class WorldGuardSync implements ClaimObserver {
    private final ClaimState claims;
    private final RegionGateway regions;
    private final AccessProjection projection;
    private final Consumer<String> warnings;
    private final Set<String> warnedFlags = new HashSet<>();

    /** @param warnings told once about each open-use flag WorldGuard doesn't know */
    public WorldGuardSync(ClaimState claims, RegionGateway regions, AccessProjection projection, Consumer<String> warnings) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.regions = Objects.requireNonNull(regions, "regions");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** Writes every Claim to WorldGuard. */
    public void syncAll() {
        claims.all().forEach(this::sync);
    }

    /** Writes the Claims the player has any right in, with any of their characters, e.g. after a profile switch. */
    public void syncPlayer(UUID player) {
        claims.all().stream().filter(claim -> involvesPlayer(claim, player)).forEach(this::sync);
    }

    /** Writes one Claim to every one of its regions. */
    public void sync(Claim claim) {
        RegionAccess access = projection.of(claim);
        for (ClaimRegion region : claim.regions()) {
            for (String flag : regions.apply(region, access)) {
                if (warnedFlags.add(claim.id() + ":" + flag)) {
                    warnings.accept("The flag '" + flag + "' of claim " + claim.id() + " (type " + claim.typeId()
                            + ") was skipped: WorldGuard doesn't know it, or doesn't accept its value");
                }
            }
        }
    }

    @Override
    public void claimChanged(ClaimChange change) {
        Set<ClaimRegion> left = new LinkedHashSet<>(change.before().map(Claim::regions).orElse(List.of()));
        change.after().ifPresent(after -> left.removeAll(after.regions()));
        // A region that leaves a Claim goes back to staff with no owners, members or owner flags;
        // the open-use flags stay.
        Set<String> beforeFlags = change.before().map(claim -> claim.flags().keySet()).orElse(Set.of());
        for (ClaimRegion region : left) {
            regions.apply(region, RegionAccess.released());
            beforeFlags.forEach(flag -> regions.clearFlag(region, flag));
        }
        change.after().ifPresent(after -> {
            beforeFlags.stream().filter(flag -> !after.flags().containsKey(flag))
                    .forEach(flag -> after.regions().forEach(region -> regions.clearFlag(region, flag)));
            sync(after);
        });
    }

    private static boolean involvesPlayer(Claim claim, UUID player) {
        return claim.owners().stream().anyMatch(owner -> owner.character().player().equals(player))
                || claim.groups().stream().flatMap(group -> group.members().stream())
                .anyMatch(member -> member.player().equals(player));
    }
}
