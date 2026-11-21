package org.skysongdev.skysongClaims.claim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimServiceTest {
    private static final ClaimRegion HOVEL_1 = new ClaimRegion("world", "hovel_1");
    private static final ClaimRegion HOVEL_2 = new ClaimRegion("world", "hovel_2");
    private static final ClaimRegion CELLAR = new ClaimRegion("world_nether", "cellar");

    private final InMemoryClaimRepository repository = new InMemoryClaimRepository();
    private final ClaimState state = new ClaimState();
    private final ClaimStore store = new ClaimStore(state, repository);
    private final List<ClaimChange> changes = new ArrayList<>();
    private final AtomicReference<ClaimTypeRegistry> types = new AtomicReference<>(TestTypes.registry());
    private ClaimService service;

    @BeforeEach
    void setUp() {
        store.observe(changes::add);
        FakeRegions regions = new FakeRegions().add("world", "hovel_1", "hovel_2").add("world_nether", "cellar");
        service = new ClaimService(store, types::get, regions);
    }

    private static RefusedException refusal(String key, org.junit.jupiter.api.function.Executable action) {
        RefusedException refusal = assertThrows(RefusedException.class, action);
        assertEquals(key, refusal.messageKey());
        return refusal;
    }

    @Test
    void createMakesAnUnownedClaimFromTheTypeDefaults() {
        Claim hovel = service.create("hovel_1", "hovel", List.of(HOVEL_1, CELLAR), ChangeCause.COMMAND);

        assertTrue(hovel.free());
        assertEquals(2, hovel.maxOwners());
        assertFalse(hovel.isOwned());
        assertEquals(List.of(HOVEL_1, CELLAR), hovel.regions());
        assertEquals(List.of(hovel), repository.loadAll());
        assertEquals(hovel, state.ofRegion(CELLAR).orElseThrow());
        assertTrue(changes.get(0).isCreate());

        Claim house = service.create("house", "other", List.of(HOVEL_2), ChangeCause.COMMAND);
        assertFalse(house.free());
        assertEquals(1, house.maxOwners());
    }

    @Test
    void createRefusesBadInput() {
        service.create("hovel_1", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);

        refusal("claim.invalid-id", () -> service.create("Bad Id", "hovel", List.of(HOVEL_2), ChangeCause.COMMAND));
        refusal("claim.id-taken", () -> service.create("hovel_1", "hovel", List.of(HOVEL_2), ChangeCause.COMMAND));
        refusal("claim.unknown-type", () -> service.create("b", "manor", List.of(HOVEL_2), ChangeCause.COMMAND));
        refusal("claim.no-regions", () -> service.create("b", "hovel", List.of(), ChangeCause.COMMAND));
        refusal("claim.region-missing", () -> service.create("b", "hovel",
                List.of(new ClaimRegion("world", "nowhere")), ChangeCause.COMMAND));
        RefusedException taken = refusal("claim.region-taken",
                () -> service.create("b", "hovel", List.of(HOVEL_2, HOVEL_1), ChangeCause.COMMAND));
        assertEquals("hovel_1", taken.placeholders().get("claim"));
        assertEquals(1, state.all().size());
    }

    @Test
    void regionsCanBeAddedAndRemoved() {
        service.create("hovel_1", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);

        service.addRegion("hovel_1", CELLAR, ChangeCause.COMMAND);
        assertEquals(List.of(HOVEL_1, CELLAR), state.find("hovel_1").orElseThrow().regions());

        service.removeRegion("hovel_1", HOVEL_1, ChangeCause.COMMAND);
        assertEquals(List.of(CELLAR), state.find("hovel_1").orElseThrow().regions());
        assertTrue(state.ofRegion(HOVEL_1).isEmpty(), "a removed region is free again");

        refusal("claim.last-region", () -> service.removeRegion("hovel_1", CELLAR, ChangeCause.COMMAND));
        refusal("claim.region-not-in-claim", () -> service.removeRegion("hovel_1", HOVEL_2, ChangeCause.COMMAND));
        refusal("claim.unknown", () -> service.addRegion("nothing", HOVEL_2, ChangeCause.COMMAND));
    }

    @Test
    void aRegionCantJoinASecondClaim() {
        service.create("a", "other", List.of(HOVEL_1), ChangeCause.COMMAND);
        service.create("b", "other", List.of(HOVEL_2), ChangeCause.COMMAND);

        refusal("claim.region-taken", () -> service.addRegion("b", HOVEL_1, ChangeCause.COMMAND));
    }

    @Test
    void typeCanChange() {
        service.create("a", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);

        assertEquals("other", service.setType("a", "other", ChangeCause.COMMAND).typeId());
        refusal("claim.unknown-type", () -> service.setType("a", "castle", ChangeCause.COMMAND));
    }

    @Test
    void freeClaimSettingsChange() {
        service.create("a", "other", List.of(HOVEL_1), ChangeCause.COMMAND);

        Claim free = service.setFree("a", true, OptionalInt.of(3), ChangeCause.COMMAND);
        assertTrue(free.free());
        assertEquals(3, free.maxOwners());

        Claim bought = service.setFree("a", false, OptionalInt.empty(), ChangeCause.COMMAND);
        assertFalse(bought.free());
        assertEquals(3, bought.maxOwners(), "slots stay when none are given");
        refusal("claim.max-owners-too-low", () -> service.setFree("a", true, OptionalInt.of(0), ChangeCause.COMMAND));
    }

    @Test
    void slotsCantDropBelowTheOwners() {
        Claim claim = service.create("a", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);
        store.save(claim.withOwners(List.of(
                new ClaimOwner(new CharacterId(UUID.randomUUID(), "A"), Instant.EPOCH, OwnerCause.STAFF),
                new ClaimOwner(new CharacterId(UUID.randomUUID(), "B"), Instant.EPOCH, OwnerCause.STAFF))), ChangeCause.COMMAND);

        refusal("claim.max-owners-below-owners", () -> service.setMaxOwners("a", 1, ChangeCause.COMMAND));
        assertEquals(5, service.setMaxOwners("a", 5, ChangeCause.COMMAND).maxOwners());
    }

    @Test
    void renameKeepsTheId() {
        service.create("a", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);

        Claim renamed = service.rename("a", "  <gold>Rose Cottage ", ChangeCause.COMMAND);

        assertEquals("<gold>Rose Cottage", renamed.displayName());
        assertEquals("a", renamed.id());
        refusal("claim.blank-name", () -> service.rename("a", " ", ChangeCause.COMMAND));
    }

    @Test
    void deleteFreesTheRegions() {
        service.create("a", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);

        service.delete("a", ChangeCause.COMMAND);

        assertTrue(state.all().isEmpty());
        assertTrue(repository.loadAll().isEmpty());
        assertTrue(changes.get(changes.size() - 1).isDelete());
        service.create("b", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);
        refusal("claim.unknown", () -> service.delete("a", ChangeCause.COMMAND));
    }

    @Test
    void listsClaimsWhoseTypeWasRemoved() {
        service.create("a", "hovel", List.of(HOVEL_1), ChangeCause.COMMAND);
        service.create("b", "other", List.of(HOVEL_2), ChangeCause.COMMAND);

        types.set(new ClaimTypeRegistry(List.of(TestTypes.OTHER)));

        assertEquals(List.of("a"), service.withUnknownType().stream().map(Claim::id).toList());
    }
}
