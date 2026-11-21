package org.skysongdev.skysongClaims.claim;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimCheckTest {
    private final FakeRegions regions = new FakeRegions().add("world", "house");
    private final FakeCharacters characters = new FakeCharacters();
    private final ClaimStore store = new ClaimStore(new ClaimState(), new InMemoryClaimRepository());
    private final ClaimCheck check = new ClaimCheck(store, regions, characters);

    private static ClaimOwner owner(CharacterId character) {
        return new ClaimOwner(character, Instant.EPOCH, OwnerCause.STAFF);
    }

    @Test
    void reportsMissingRegionsWithoutRemovingThem() {
        store.save(Claim.create("house", "other", false, 1,
                List.of(new ClaimRegion("world", "house"), new ClaimRegion("world", "gone"))), ChangeCause.COMMAND);

        ClaimCheck.Report report = check.run();

        assertEquals(List.of(new ClaimCheck.Missing("house", new ClaimRegion("world", "gone"))), report.missingRegions());
        assertEquals(2, store.require("house").regions().size());
    }

    @Test
    void releasesGoneCharactersFromOwnersAndGroups() {
        CharacterId alice = characters.play(UUID.randomUUID(), "Alice");
        CharacterId bob = characters.play(UUID.randomUUID(), "Bob");
        CharacterId carol = characters.play(UUID.randomUUID(), "Carol");
        store.save(Claim.create("house", "other", false, 2, List.of(new ClaimRegion("world", "house")))
                .withOwners(List.of(owner(alice), owner(bob)))
                .withGroups(List.of(new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(carol)))),
                ChangeCause.COMMAND);
        characters.delete(bob);
        characters.delete(carol);
        List<ClaimChange> changes = new ArrayList<>();
        store.observe(changes::add);

        ClaimCheck.Report report = check.run();

        Claim house = store.require("house");
        assertEquals(List.of(alice), house.owners().stream().map(ClaimOwner::character).toList());
        assertTrue(house.groups().get(0).members().isEmpty());
        assertEquals(2, report.released().size());
        assertEquals(ChangeCause.RECONCILE, changes.get(0).cause());
    }

    @Test
    void releasingTheLastOwnerLeavesTheClaimUnownedWithNoGroups() {
        CharacterId alice = characters.play(UUID.randomUUID(), "Alice");
        CharacterId bob = characters.play(UUID.randomUUID(), "Bob");
        store.save(Claim.create("house", "hovel", true, 2, List.of(new ClaimRegion("world", "house")))
                .withOwners(List.of(owner(alice)))
                .withGroups(List.of(new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(bob)))),
                ChangeCause.COMMAND);
        characters.delete(alice);

        check.run();

        Claim house = store.require("house");
        assertTrue(house.owners().isEmpty());
        assertTrue(house.groups().isEmpty());
        assertEquals(2, house.openSlots());
    }

    @Test
    void aCleanClaimIsLeftAlone() {
        CharacterId alice = characters.play(UUID.randomUUID(), "Alice");
        store.save(Claim.create("house", "other", false, 1, List.of(new ClaimRegion("world", "house")))
                .withOwners(List.of(owner(alice))), ChangeCause.COMMAND);
        List<ClaimChange> changes = new ArrayList<>();
        store.observe(changes::add);

        ClaimCheck.Report report = check.run();

        assertTrue(report.released().isEmpty());
        assertTrue(changes.isEmpty());
    }
}
