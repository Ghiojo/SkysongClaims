package org.skysongdev.skysongClaims.limit;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimOwner;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.OwnerCause;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.LimitScope;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitsTest {
    private static final UUID PLAYER = UUID.randomUUID();
    private static final CharacterId ALICE = new CharacterId(PLAYER, "Alice");
    private static final CharacterId ALICIA = new CharacterId(PLAYER, "Alicia");
    private static final ClaimType MANOR = new ClaimType("manor", "Manor", 1, LimitScope.ACCOUNT, 1, false, List.of(), Map.of());

    private final List<String> failures = new ArrayList<>();
    private final LimitRules rules = new LimitRules((what, e) -> failures.add(what));
    private final ClaimState state = new ClaimState();
    private final Limits limits = new Limits(state, rules);

    private void own(String id, String type, CharacterId... owners) {
        List<ClaimOwner> list = new ArrayList<>();
        for (CharacterId owner : owners) {
            list.add(new ClaimOwner(owner, Instant.EPOCH, OwnerCause.STAFF));
        }
        state.load(concat(state.all(), Claim.create(id, type, false, 3, List.of(new ClaimRegion("world", id))).withOwners(list)));
    }

    private static List<Claim> concat(List<Claim> claims, Claim claim) {
        List<Claim> all = new ArrayList<>(claims);
        all.add(claim);
        return all;
    }

    @Test
    void perCharacterLimitsCountOnlyThatCharacter() {
        own("a", "hovel", ALICE);
        LimitSubject alice = Limits.subject(ALICE, TestTypes.HOVEL);

        assertEquals(1, limits.count(alice, TestTypes.HOVEL));
        assertEquals(0, limits.remaining(alice, TestTypes.HOVEL));
        assertEquals(1, limits.remaining(Limits.subject(ALICIA, TestTypes.HOVEL), TestTypes.HOVEL));
    }

    @Test
    void perAccountLimitsCountEveryCharacterOfThePlayer() {
        own("a", "manor", ALICE);
        own("b", "manor", ALICIA);
        LimitSubject account = Limits.subject(ALICIA, MANOR);

        assertEquals(LimitSubject.ofAccount(PLAYER), account);
        assertEquals(2, limits.count(account, MANOR));
        assertEquals(0, limits.remaining(account, MANOR), "over the limit is never negative");
    }

    @Test
    void aSharedClaimCountsOnceForEachOwner() {
        CharacterId bob = new CharacterId(UUID.randomUUID(), "Bob");
        own("a", "hovel", ALICE, bob);

        assertEquals(1, limits.count(LimitSubject.ofCharacter(ALICE), TestTypes.HOVEL));
        assertEquals(1, limits.count(LimitSubject.ofCharacter(bob), TestTypes.HOVEL));
    }

    @Test
    void liveRulesRaiseTheLimitUntilTheirPluginGoes() {
        rules.register("Ranks", "noble", (subject, type) -> type.id().equals("hovel") ? 1 : 0);
        LimitSubject alice = LimitSubject.ofCharacter(ALICE);

        assertEquals(2, limits.limit(alice, TestTypes.HOVEL));
        assertEquals(3, limits.limit(alice, TestTypes.OTHER));

        rules.unregisterAll("Ranks");
        assertEquals(1, limits.limit(alice, TestTypes.HOVEL));
    }

    @Test
    void aFailingRuleCountsAsNothing() {
        rules.register("Broken", "boom", (subject, type) -> {
            throw new IllegalStateException("boom");
        });
        rules.register("Ranks", "minus", (subject, type) -> -5);

        assertEquals(0, limits.limit(LimitSubject.ofCharacter(ALICE), TestTypes.HOVEL), "never below 0");
        assertEquals(1, failures.size());
    }

    @Test
    void aSubjectsCharacterIsTheirs() {
        assertThrows(IllegalArgumentException.class,
                () -> new LimitSubject(UUID.randomUUID(), java.util.Optional.of(ALICE)));
        assertTrue(LimitSubject.ofAccount(PLAYER).counts(ALICIA));
    }
}
