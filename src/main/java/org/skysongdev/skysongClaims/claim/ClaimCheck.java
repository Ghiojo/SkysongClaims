package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.character.CharacterDirectory;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Finds what changed behind SkysongClaims' back, since neither WorldGuard nor SkysongStats
 * says so: regions deleted from WorldGuard are reported (never removed on their own), and
 * characters that no longer exist are released from every Claim they owned or were a group
 * member of. Runs at start-up and with {@code /sclaims check}.
 */
public final class ClaimCheck {
    /**
     * What a check found.
     *
     * @param missingRegions each Claim region WorldGuard no longer has
     * @param released       each character released from a Claim, by Claim id
     */
    public record Report(List<Missing> missingRegions, List<Released> released) {
        public Report {
            missingRegions = List.copyOf(missingRegions);
            released = List.copyOf(released);
        }
    }

    /** A Claim region WorldGuard no longer has. */
    public record Missing(String claimId, ClaimRegion region) {
    }

    /** A character that no longer exists, taken out of a Claim. */
    public record Released(String claimId, CharacterId character) {
    }

    private final ClaimStore store;
    private final RegionGateway regions;
    private final CharacterDirectory characters;

    public ClaimCheck(ClaimStore store, RegionGateway regions, CharacterDirectory characters) {
        this.store = Objects.requireNonNull(store, "store");
        this.regions = Objects.requireNonNull(regions, "regions");
        this.characters = Objects.requireNonNull(characters, "characters");
    }

    public Report run() {
        List<Missing> missing = new ArrayList<>();
        List<Released> released = new ArrayList<>();
        for (Claim claim : store.state().all()) {
            claim.regions().stream().filter(region -> !regions.exists(region))
                    .forEach(region -> missing.add(new Missing(claim.id(), region)));
            Claim cleaned = claim;
            for (CharacterId gone : goneCharacters(claim)) {
                cleaned = cleaned.withoutCharacter(gone);
                released.add(new Released(claim.id(), gone));
            }
            store.save(cleaned, ChangeCause.RECONCILE);
        }
        return new Report(missing, released);
    }

    private Set<CharacterId> goneCharacters(Claim claim) {
        Set<CharacterId> involved = new LinkedHashSet<>();
        claim.owners().forEach(owner -> involved.add(owner.character()));
        claim.groups().forEach(group -> involved.addAll(group.members()));
        involved.removeIf(characters::exists);
        return involved;
    }
}
