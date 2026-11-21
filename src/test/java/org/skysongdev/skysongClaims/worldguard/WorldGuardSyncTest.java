package org.skysongdev.skysongClaims.worldguard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongClaims.type.ClaimTypesParser;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldGuardSyncTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    private final FakeRegions regions = new FakeRegions().add("world", "house", "garden", "shed");
    private final FakeCharacters characters = new FakeCharacters();
    private final ClaimState state = new ClaimState();
    private final ClaimStore store = new ClaimStore(state, new InMemoryClaimRepository());
    private final List<String> warnings = new ArrayList<>();
    private WorldGuardSync sync;
    private CharacterId alice;
    private CharacterId aliceAlt;
    private CharacterId bob;

    @BeforeEach
    void setUp() {
        sync = new WorldGuardSync(state, regions, new AccessProjection(characters, TestTypes::registry), warnings::add);
        store.observe(sync);
        alice = characters.play(ALICE, "Alice");
        aliceAlt = characters.add(ALICE, "Alicia");
        bob = characters.play(BOB, "Bob");
    }

    private Claim house(List<ClaimOwner> owners, List<ClaimGroup> groups) {
        return Claim.create("house", "other", false, 2,
                List.of(new ClaimRegion("world", "house"), new ClaimRegion("world", "garden")))
                .withOwners(owners).withGroups(groups);
    }

    private static ClaimOwner owner(CharacterId character) {
        return new ClaimOwner(character, Instant.EPOCH, OwnerCause.STAFF);
    }

    @Test
    void everyRegionGetsTheOwnersBuildersAndOpenUseFlags() {
        store.save(house(List.of(owner(alice)),
                List.of(new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(bob)))), ChangeCause.COMMAND);

        RegionAccess expected = new RegionAccess(Set.of(ALICE), Set.of(BOB), ClaimTypesParser.DEFAULT_OPEN_USE, java.util.Map.of());
        assertEquals(expected, regions.applied("world", "house"));
        assertEquals(expected, regions.applied("world", "garden"));
    }

    @Test
    void anUnownedClaimStillOpensDoorsAndChests() {
        store.save(house(List.of(), List.of()), ChangeCause.COMMAND);

        assertEquals(new RegionAccess(Set.of(), Set.of(), ClaimTypesParser.DEFAULT_OPEN_USE, java.util.Map.of()), regions.applied("world", "house"));
    }

    @Test
    void groupsWithoutBuildGiveNothing() {
        store.save(house(List.of(owner(alice)), List.of(new ClaimGroup("guests", "Guests", Set.of(), Set.of(bob)))),
                ChangeCause.COMMAND);

        assertEquals(Set.of(), regions.applied("world", "house").members());
    }

    @Test
    void rightsFollowTheCharacterBeingPlayed() {
        store.save(house(List.of(owner(aliceAlt)), List.of()), ChangeCause.COMMAND);
        assertEquals(Set.of(), regions.applied("world", "house").owners(), "Alice plays another character");

        characters.play(ALICE, "Alicia");
        sync.syncPlayer(ALICE);

        assertEquals(Set.of(ALICE), regions.applied("world", "house").owners());
    }

    @Test
    void anOwnerInAGroupIsOnlyAnOwner() {
        store.save(house(List.of(owner(alice)),
                List.of(new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(alice)))), ChangeCause.COMMAND);

        assertEquals(Set.of(), regions.applied("world", "house").members());
    }

    @Test
    void aRegionLeavingTheClaimIsReleased() {
        Claim house = house(List.of(owner(alice)), List.of());
        store.save(house, ChangeCause.COMMAND);

        store.save(house.withRegions(List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);

        assertEquals(RegionAccess.released(), regions.applied("world", "garden"));
        assertEquals(Set.of(ALICE), regions.applied("world", "house").owners());
    }

    @Test
    void aDeletedClaimReleasesEveryRegion() {
        store.save(house(List.of(owner(alice)), List.of()), ChangeCause.COMMAND);

        store.delete("house", ChangeCause.COMMAND);

        assertEquals(RegionAccess.released(), regions.applied("world", "house"));
        assertEquals(RegionAccess.released(), regions.applied("world", "garden"));
    }

    @Test
    void unknownFlagsAreReportedOnce() {
        regions.unknownFlag("use-dripleaf");
        store.save(house(List.of(), List.of()), ChangeCause.COMMAND);

        sync.syncAll();

        assertEquals(1, warnings.size());
    }

    @Test
    void aClaimWithARemovedTypeKeepsTheStandardOpenUse() {
        store.save(Claim.create("old", "castle", false, 1, List.of(new ClaimRegion("world", "shed"))), ChangeCause.COMMAND);

        assertEquals(ClaimTypesParser.DEFAULT_OPEN_USE, regions.applied("world", "shed").openUse());
    }
}
