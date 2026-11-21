package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.api.event.ClaimCreateEvent;
import org.skysongdev.skysongClaims.api.event.ClaimDeleteEvent;
import org.skysongdev.skysongClaims.api.event.ClaimOwnerAddEvent;
import org.skysongdev.skysongClaims.api.event.ClaimOwnerRemoveEvent;
import org.skysongdev.skysongClaims.api.event.ClaimSaleChangeEvent;
import org.skysongdev.skysongClaims.api.event.ClaimSignChangeEvent;
import org.skysongdev.skysongClaims.api.event.ClaimTypeChangeEvent;
import org.skysongdev.skysongClaims.api.event.SkysongClaimsReloadEvent;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.SignLocation;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBridgeTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Alice");
    private static final CharacterId BOB = new CharacterId(UUID.randomUUID(), "Bob");

    private final List<Event> fired = new ArrayList<>();
    private final ClaimStore store = new ClaimStore(new ClaimState(), new InMemoryClaimRepository());
    private final EventBridge bridge = new EventBridge(fired::add);

    @BeforeEach
    void setUp() {
        store.observe(bridge);
        store.save(Claim.create("house", "other", false, 2, List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);
    }

    private static ClaimOwner owner(CharacterId character) {
        return new ClaimOwner(character, Instant.EPOCH, OwnerCause.API);
    }

    private void owners(ChangeCause cause, CharacterId... characters) {
        store.save(store.require("house").withOwners(java.util.Arrays.stream(characters).map(EventBridgeTest::owner).toList()),
                cause);
    }

    @Test
    void firesCreateAndDelete() {
        ClaimCreateEvent created = assertInstanceOf(ClaimCreateEvent.class, fired.getFirst());
        assertEquals("house", created.getClaim().id());
        assertEquals(ChangeCause.COMMAND, created.getCause());

        store.delete("house", ChangeCause.COMMAND);
        assertEquals("house", assertInstanceOf(ClaimDeleteEvent.class, fired.getLast()).getClaim().id());
    }

    @Test
    void firesOneEventPerOwnerAddedOrRemovedWithTheCause() {
        fired.clear();
        owners(ChangeCause.API, ALICE);
        ClaimOwnerAddEvent added = assertInstanceOf(ClaimOwnerAddEvent.class, fired.getFirst());
        assertEquals(ALICE, added.getCharacter());
        assertEquals(ChangeCause.API, added.getCause());
        assertTrue(added.getClaim().isOwnedBy(ALICE));

        fired.clear();
        owners(ChangeCause.LEAVE);
        ClaimOwnerRemoveEvent removed = assertInstanceOf(ClaimOwnerRemoveEvent.class, fired.getFirst());
        assertEquals(ALICE, removed.getCharacter());
        assertEquals(ChangeCause.LEAVE, removed.getCause());
    }

    @Test
    void aTransferIsARemovalThenAnAddition() {
        owners(ChangeCause.API, ALICE);
        fired.clear();

        owners(ChangeCause.API, BOB);

        assertEquals(2, fired.size());
        assertEquals(ALICE, assertInstanceOf(ClaimOwnerRemoveEvent.class, fired.get(0)).getCharacter());
        assertEquals(BOB, assertInstanceOf(ClaimOwnerAddEvent.class, fired.get(1)).getCharacter());
    }

    @Test
    void firesSaleAndTypeChanges() {
        fired.clear();
        store.save(store.require("house").withSale(Optional.of("SkysongEconomy")), ChangeCause.API);
        assertTrue(assertInstanceOf(ClaimSaleChangeEvent.class, fired.getLast()).getClaim().isForSale());

        store.save(store.require("house").withType("hovel"), ChangeCause.COMMAND);
        ClaimTypeChangeEvent typed = assertInstanceOf(ClaimTypeChangeEvent.class, fired.getLast());
        assertEquals("other", typed.getPreviousTypeId());
        assertEquals("hovel", typed.getClaim().typeId());
    }

    @Test
    void otherChangesFireNothing() {
        fired.clear();
        store.save(store.require("house").withDisplayName("Rose House"), ChangeCause.COMMAND);
        assertTrue(fired.isEmpty());
    }

    @Test
    void firesSignAndReloadEvents() {
        bridge.signChanged(new ClaimSign("house", new SignLocation("world", 1, 2, 3), Optional.empty()), true);
        ClaimSignChangeEvent sign = assertInstanceOf(ClaimSignChangeEvent.class, fired.getLast());
        assertTrue(sign.isLinked());
        assertEquals(3, sign.getSign().z());

        bridge.reloaded();
        assertInstanceOf(SkysongClaimsReloadEvent.class, fired.getLast());
    }
}
