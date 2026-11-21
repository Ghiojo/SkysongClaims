package org.skysongdev.skysongClaims.ownership;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.claim.OwnerCause;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.MutableClock;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteServiceTest {
    private final FakeCharacters characters = new FakeCharacters();
    private final ClaimStore store = new ClaimStore(new ClaimState(), new InMemoryClaimRepository());
    private final MutableClock clock = new MutableClock();
    private final OwnershipService ownership = new OwnershipService(store, TestTypes::registry,
            new Limits(store.state(), new LimitRules((what, e) -> {
            })), characters, clock);
    private final InviteService invites = new InviteService(store, ownership, () -> Duration.ofMinutes(5), clock);
    private CharacterId alice;
    private CharacterId bob;

    @BeforeEach
    void setUp() {
        alice = characters.play(UUID.randomUUID(), "Alice");
        bob = characters.play(UUID.randomUUID(), "Bob");
        store.save(Claim.create("house", "other", false, 2, List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);
        store.save(Claim.create("shop", "other", false, 2, List.of(new ClaimRegion("world", "shop"))), ChangeCause.COMMAND);
        ownership.addOwner("house", alice, OwnerCause.STAFF, ChangeCause.COMMAND, false);
        ownership.addOwner("shop", alice, OwnerCause.STAFF, ChangeCause.COMMAND, false);
    }

    private static String refusal(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(RefusedException.class, action).messageKey();
    }

    @Test
    void anAcceptedInviteMakesACoOwner() {
        invites.invite(alice, "house", bob);

        Claim claim = invites.accept(bob, Optional.empty());

        assertTrue(claim.isOwnedBy(bob));
        assertEquals(OwnerCause.INVITE, claim.owners().get(1).cause());
        assertTrue(invites.pending(bob).isEmpty());
    }

    @Test
    void onlyOwnersInvite() {
        assertEquals("ownership.not-owner", refusal(() -> invites.invite(bob, "house", alice)));
    }

    @Test
    void invitesThatCouldNeverBeAcceptedAreRefusedAtOnce() {
        assertEquals("ownership.already-owner", refusal(() -> invites.invite(alice, "house", alice)));
    }

    @Test
    void invitesExpire() {
        invites.invite(alice, "house", bob);

        clock.advance(Duration.ofMinutes(5));

        assertEquals("ownership.no-invite", refusal(() -> invites.accept(bob, Optional.empty())));
    }

    @Test
    void severalInvitesNeedAClaimId() {
        invites.invite(alice, "house", bob);
        invites.invite(alice, "shop", bob);

        assertEquals("ownership.which-invite", refusal(() -> invites.accept(bob, Optional.empty())));
        assertEquals("shop", invites.deny(bob, Optional.of("shop")).claimId());
        assertTrue(invites.accept(bob, Optional.empty()).isOwnedBy(bob));
    }

    @Test
    void acceptingChecksAgain() {
        invites.invite(alice, "house", bob);
        CharacterId carol = characters.play(UUID.randomUUID(), "Carol");
        invites.invite(alice, "house", carol);
        invites.accept(carol, Optional.empty());

        assertEquals("ownership.full", refusal(() -> invites.accept(bob, Optional.empty())));
    }
}
