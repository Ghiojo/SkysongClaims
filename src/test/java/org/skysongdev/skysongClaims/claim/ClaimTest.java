package org.skysongdev.skysongClaims.claim;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimTest {
    private static final ClaimRegion REGION = new ClaimRegion("world", "Hovel_1");
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Alice");

    @Test
    void aNewClaimIsUnownedAndNamedAfterItsId() {
        Claim claim = Claim.create("hovel_1", "hovel", OptionalInt.of(2), List.of(REGION));

        assertEquals("hovel_1", claim.displayName());
        assertFalse(claim.isOwned());
        assertFalse(claim.isForSale());
        assertEquals(OptionalInt.of(2), claim.openSlots());
    }

    @Test
    void regionIdsAreLowerCase() {
        assertEquals("hovel_1", REGION.regionId());
        assertEquals("world:hovel_1", REGION.toString());
    }

    @Test
    void ownersFillSlots() {
        Claim claim = Claim.create("hovel_1", "hovel", OptionalInt.of(1), List.of(REGION))
                .withOwners(List.of(new ClaimOwner(ALICE, Instant.EPOCH, OwnerCause.FREE_CLAIM)));

        assertTrue(claim.isOwnedBy(ALICE));
        assertEquals(OptionalInt.of(0), claim.openSlots());
    }

    @Test
    void findsGroupsById() {
        ClaimGroup family = new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), Set.of(ALICE));
        Claim claim = Claim.create("a", "other", OptionalInt.empty(), List.of(REGION)).withGroups(List.of(family));

        assertEquals(Optional.of(family), claim.group("family"));
        assertTrue(claim.group("friends").isEmpty());
        assertTrue(family.has(GroupPermission.BUILD));
    }

    @Test
    void saleNamesThePluginThatSetIt() {
        Claim claim = Claim.create("a", "other", OptionalInt.empty(), List.of(REGION)).withSale(Optional.of("SkysongEconomy"));

        assertTrue(claim.isForSale());
    }

    @Test
    void duplicateRegionsAreKeptOnce() {
        assertEquals(1, Claim.create("a", "other", OptionalInt.empty(), List.of(REGION, new ClaimRegion("world", "hovel_1")))
                .regions().size());
    }

    @Test
    void refusesInvalidClaims() {
        assertThrows(IllegalArgumentException.class, () -> Claim.create("Bad Id", "other", OptionalInt.empty(), List.of(REGION)));
        assertThrows(IllegalArgumentException.class, () -> Claim.create("a", "other", OptionalInt.empty(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> Claim.create("a", "hovel", OptionalInt.of(0), List.of(REGION)));
    }

    @Test
    void aBoughtClaimAlwaysHasRoom() {
        Claim claim = Claim.create("a", "other", OptionalInt.empty(), List.of(REGION)).withOwners(List.of(
                new ClaimOwner(new CharacterId(UUID.randomUUID(), "A"), Instant.EPOCH, OwnerCause.STAFF),
                new ClaimOwner(new CharacterId(UUID.randomUUID(), "B"), Instant.EPOCH, OwnerCause.STAFF)));

        assertFalse(claim.free());
        assertTrue(claim.hasRoom());
        assertTrue(claim.openSlots().isEmpty());
    }

    @Test
    void groupPermissionsHaveConfigNames() {
        assertEquals(Optional.of(GroupPermission.BUILD), GroupPermission.fromName("Build"));
        assertTrue(GroupPermission.fromName("open-chests").isEmpty());
    }
}
