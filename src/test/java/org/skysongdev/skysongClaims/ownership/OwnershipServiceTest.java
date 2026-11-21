package org.skysongdev.skysongClaims.ownership;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.OwnershipCheck;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.MutableClock;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnershipServiceTest {
    private final FakeCharacters characters = new FakeCharacters();
    private final ClaimStore store = new ClaimStore(new ClaimState(), new InMemoryClaimRepository());
    private final LimitRules rules = new LimitRules((what, e) -> {
    });
    private final MutableClock clock = new MutableClock();
    private final OwnershipService ownership = new OwnershipService(store, TestTypes::registry,
            new Limits(store.state(), rules), characters, clock);
    private CharacterId alice;
    private CharacterId bob;
    private CharacterId carol;

    @BeforeEach
    void setUp() {
        alice = characters.play(UUID.randomUUID(), "Alice");
        bob = characters.play(UUID.randomUUID(), "Bob");
        carol = characters.play(UUID.randomUUID(), "Carol");
        store.save(Claim.create("hovel_1", "hovel", true, 2, List.of(new ClaimRegion("world", "hovel_1"))), ChangeCause.COMMAND);
        store.save(Claim.create("hovel_2", "hovel", true, 2, List.of(new ClaimRegion("world", "hovel_2"))), ChangeCause.COMMAND);
        store.save(Claim.create("house", "other", false, 1, List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);
    }

    private static String refusal(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(RefusedException.class, action).messageKey();
    }

    @Test
    void eachSlotOfAFreeClaimIsTakenOnItsOwn() {
        ownership.take(alice, "hovel_1");
        Claim full = ownership.take(bob, "hovel_1");

        assertEquals(List.of(alice, bob), full.owners().stream().map(ClaimOwner::character).toList());
        assertEquals(OwnerCause.FREE_CLAIM, full.owners().get(0).cause());
        assertEquals(clock.instant(), full.owners().get(0).since());
        assertEquals("ownership.full", refusal(() -> ownership.take(carol, "hovel_1")));
    }

    @Test
    void theTypeLimitStopsASecondHovel() {
        ownership.take(alice, "hovel_1");

        assertEquals(OwnershipCheck.TYPE_LIMIT, ownership.canOwn(alice, store.require("hovel_2")));
        assertEquals("ownership.type-limit", refusal(() -> ownership.take(alice, "hovel_2")));

        rules.register("Ranks", "noble", (subject, type) -> 1);
        assertEquals(OwnershipCheck.OK, ownership.canOwn(alice, store.require("hovel_2")));
    }

    @Test
    void onlyFreeClaimsCanBeTaken() {
        assertEquals("ownership.not-free", refusal(() -> ownership.take(alice, "house")));
    }

    @Test
    void checksInOrder() {
        ownership.take(alice, "hovel_1");
        CharacterId ghost = new CharacterId(UUID.randomUUID(), "Ghost");

        assertEquals(OwnershipCheck.ALREADY_OWNER, ownership.canOwn(alice, store.require("hovel_1")));
        assertEquals(OwnershipCheck.UNKNOWN_CHARACTER, ownership.canOwn(ghost, store.require("hovel_1")));
        assertEquals(OwnershipCheck.OK, ownership.canOwn(bob, store.require("hovel_1")));
    }

    @Test
    void staffCanForcePastTheLimitButNotPastTheSlots() {
        ownership.take(alice, "hovel_1");

        Claim forced = ownership.addOwner("hovel_2", alice, OwnerCause.STAFF, ChangeCause.COMMAND, true);
        assertTrue(forced.isOwnedBy(alice));

        ownership.addOwner("house", bob, OwnerCause.STAFF, ChangeCause.COMMAND, false);
        assertEquals("ownership.full",
                refusal(() -> ownership.addOwner("house", carol, OwnerCause.STAFF, ChangeCause.COMMAND, true)));
    }

    @Test
    void anOwnerLeavingKeepsTheOthersAndTheGroups() {
        ownership.take(alice, "hovel_1");
        Claim shared = ownership.take(bob, "hovel_1");
        store.save(shared.withGroups(List.of(new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(carol)))),
                ChangeCause.COMMAND);

        Claim left = ownership.removeOwner("hovel_1", alice, ChangeCause.COMMAND);

        assertFalse(left.isOwnedBy(alice));
        assertTrue(left.isOwnedBy(bob));
        assertEquals(1, left.groups().size());
        assertEquals(1, left.openSlots());
    }

    @Test
    void theLastOwnerLeavingReopensTheClaimAndClearsGroups() {
        Claim taken = ownership.take(alice, "hovel_1");
        store.save(taken.withGroups(List.of(new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(carol)))),
                ChangeCause.COMMAND);

        Claim left = ownership.removeOwner("hovel_1", alice, ChangeCause.COMMAND);

        assertFalse(left.isOwned());
        assertTrue(left.groups().isEmpty());
        assertEquals(2, left.openSlots());
        assertEquals("ownership.not-owner", refusal(() -> ownership.removeOwner("hovel_1", alice, ChangeCause.COMMAND)));
    }

    @Test
    void someoneOverTheirLimitKeepsWhatTheyOwn() {
        ownership.take(alice, "hovel_1");
        rules.register("Ranks", "noble", (subject, type) -> 1);
        ownership.take(alice, "hovel_2");

        rules.unregisterAll("Ranks");

        assertEquals(2, ownership.ownedBy(alice).size());
    }
}
