package org.skysongdev.skysongClaims.flag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.group.Manager;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongClaims.worldguard.AccessProjection;
import org.skysongdev.skysongClaims.worldguard.WorldGuardSync;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagServiceTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Alice");

    private final FakeRegions regions = new FakeRegions().add("world", "house", "garden");
    private final ClaimState state = new ClaimState();
    private final ClaimStore store = new ClaimStore(state, new InMemoryClaimRepository());
    private final FlagService flags = new FlagService(store, () -> List.of("greeting", "farewell", "build", "chest-access"),
            TestTypes::registry, regions);
    private final Manager alice = Manager.owner(ALICE);

    @BeforeEach
    void setUp() {
        store.observe(new WorldGuardSync(state, regions, new AccessProjection(new FakeCharacters(), TestTypes::registry),
                warning -> {
                }));
        store.save(Claim.create("house", "other", OptionalInt.empty(),
                        List.of(new ClaimRegion("world", "house"), new ClaimRegion("world", "garden")))
                .withOwners(List.of(new ClaimOwner(ALICE, Instant.EPOCH, OwnerCause.STAFF))), ChangeCause.COMMAND);
    }

    private static String refusal(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(RefusedException.class, action).messageKey();
    }

    @Test
    void buildAndOpenUseFlagsAreNeverEditableEvenWhenListed() {
        assertEquals(List.of("greeting", "farewell"), flags.editable(store.require("house")));
        assertEquals("flag.not-editable", refusal(() -> flags.set(alice, "house", "chest-access", "deny")));
        assertEquals("flag.not-editable", refusal(() -> flags.set(alice, "house", "build", "allow")));
    }

    @Test
    void aSetFlagReachesEveryRegion() {
        flags.set(alice, "house", "Greeting", "Welcome");

        assertEquals(Map.of("greeting", "Welcome"), store.require("house").flags());
        assertEquals(Map.of("greeting", "Welcome"), regions.applied("world", "house").flags());
        assertEquals(Map.of("greeting", "Welcome"), regions.applied("world", "garden").flags());
    }

    @Test
    void clearingRemovesItFromEveryRegion() {
        flags.set(alice, "house", "greeting", "Welcome");

        flags.clear(alice, "house", "greeting");

        assertTrue(store.require("house").flags().isEmpty());
        assertEquals(Set.of("world:house/greeting", "world:garden/greeting"), regions.cleared());
    }

    @Test
    void aRegionLeavingTheClaimLosesTheOwnersFlags() {
        flags.set(alice, "house", "greeting", "Welcome");
        Claim house = store.require("house");

        store.save(house.withRegions(List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);

        assertEquals(Set.of("world:garden/greeting"), regions.cleared());
    }

    @Test
    void worldGuardMustTakeTheValue() {
        assertEquals("flag.bad-value", refusal(() -> flags.set(alice, "house", "greeting", "bad")));
    }

    @Test
    void onlyOwnersAndStaffSetFlags() {
        assertEquals("ownership.not-owner",
                refusal(() -> flags.set(Manager.owner(new CharacterId(UUID.randomUUID(), "Bob")), "house", "greeting", "Hi")));
        flags.set(Manager.staff(), "house", "farewell", "Bye");
    }
}
