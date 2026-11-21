package org.skysongdev.skysongClaims.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimOwner;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.claim.OwnerCause;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.support.FakeSignBlocks;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignServiceTest {
    private static final SignLocation BOARD = new SignLocation("world", 10, 64, 10);
    private static final SignLocation DOOR = new SignLocation("world", 100, 64, 100);
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Alice");

    private final ClaimState state = new ClaimState();
    private final ClaimStore store = new ClaimStore(state, new InMemoryClaimRepository());
    private final InMemorySignRepository repository = new InMemorySignRepository();
    private final FakeSignBlocks blocks = new FakeSignBlocks().sign(BOARD).sign(DOOR);
    private final List<String> log = new ArrayList<>();
    private final List<String> heard = new ArrayList<>();
    private SignService signs;

    @BeforeEach
    void setUp() {
        signs = new SignService(state, repository, blocks, new SignText(TestMessages.messenger(), TestTypes::registry), log::add);
        signs.listen((sign, linked) -> heard.add((linked ? "+" : "-") + sign.location()));
        store.observe(signs);
        store.save(Claim.create("hovel_1", "hovel", true, 2, List.of(new ClaimRegion("world", "hovel_1")))
                .withDisplayName("<gold>Rose Hovel"), ChangeCause.COMMAND);
        store.save(Claim.create("house", "other", false, 1, List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);
    }

    private static List<ClaimOwner> owners(CharacterId... characters) {
        return java.util.Arrays.stream(characters).map(c -> new ClaimOwner(c, Instant.EPOCH, OwnerCause.STAFF)).toList();
    }

    @Test
    void aFreeClaimSignShowsNameTypeAndOpenSlots() {
        signs.link("hovel_1", BOARD);

        assertEquals(List.of("Rose Hovel", "Hovel", "2 of 2 open", ""), blocks.lines(BOARD));
        assertEquals(List.of("+" + BOARD), heard);
        assertEquals(1, repository.loadAll().size());
    }

    @Test
    void linesFollowTheClaim() {
        signs.link("hovel_1", BOARD);
        Claim hovel = store.require("hovel_1");

        store.save(hovel.withOwners(owners(ALICE)), ChangeCause.COMMAND);
        assertEquals("1 of 2 open", blocks.lines(BOARD).get(2));

        store.save(hovel.withOwners(owners(ALICE, new CharacterId(UUID.randomUUID(), "Bob"))), ChangeCause.COMMAND);
        assertEquals("Full", blocks.lines(BOARD).get(2));
    }

    @Test
    void aBoughtClaimSignSaysForSaleOrOwned() {
        signs.link("house", DOOR);
        assertEquals("", blocks.lines(DOOR).get(2));

        Claim house = store.require("house");
        store.save(house.withSale(Optional.of("SkysongEconomy")), ChangeCause.API);
        assertEquals("For sale", blocks.lines(DOOR).get(2));

        store.save(house.withOwners(owners(ALICE)), ChangeCause.API);
        assertEquals("Owned", blocks.lines(DOOR).get(2));
    }

    @Test
    void thePriceLineIsSavedAndShownOnEverySignOfTheClaim() {
        signs.link("house", DOOR);
        signs.link("house", BOARD);

        signs.setPriceLine("house", Optional.of(GsonComponentSerializer.gson().serialize(Component.text("500 gold"))));

        assertEquals("500 gold", blocks.lines(DOOR).get(3));
        assertEquals("500 gold", blocks.lines(BOARD).get(3));
        assertTrue(repository.loadAll().stream().allMatch(sign -> sign.priceLine().isPresent()));
    }

    @Test
    void unlinkingReleasesTheSign() {
        signs.link("house", DOOR);

        signs.unlink(DOOR);

        assertTrue(blocks.released(DOOR));
        assertTrue(signs.of("house").isEmpty());
        assertTrue(repository.loadAll().isEmpty());
        assertEquals("signs.not-linked", assertThrows(RefusedException.class, () -> signs.unlink(DOOR)).messageKey());
    }

    @Test
    void relinkingASignMovesIt() {
        signs.link("house", DOOR);

        signs.link("hovel_1", DOOR);

        assertEquals("hovel_1", signs.at(DOOR).orElseThrow().claimId());
        assertEquals(List.of("+" + DOOR, "-" + DOOR, "+" + DOOR), heard);
    }

    @Test
    void onlySignsCanBeLinked() {
        RefusedException refusal = assertThrows(RefusedException.class,
                () -> signs.link("house", new SignLocation("world", 0, 0, 0)));

        assertEquals("signs.not-a-sign", refusal.messageKey());
        assertEquals("claim.unknown", assertThrows(RefusedException.class, () -> signs.link("castle", DOOR)).messageKey());
    }

    @Test
    void aSignWhoseBlockIsGoneIsUnlinkedAndLogged() {
        signs.link("house", DOOR);
        blocks.breakSign(DOOR);

        signs.redrawAll();

        assertTrue(signs.of("house").isEmpty());
        assertTrue(repository.loadAll().isEmpty());
        assertEquals(1, log.size());
    }

    @Test
    void anUnloadedSignIsLeftForItsChunkLoad() {
        signs.link("house", DOOR);
        blocks.unload(DOOR);
        store.save(store.require("house").withOwners(owners(ALICE)), ChangeCause.COMMAND);
        assertEquals(1, signs.of("house").size(), "kept while its chunk is unloaded");

        signs.load();
        assertEquals(1, signs.of("house").size());
    }

    @Test
    void chunkLoadsRedrawOnlyTheirSigns() {
        signs.link("house", DOOR);
        signs.link("hovel_1", BOARD);
        blocks.breakSign(BOARD);

        signs.redrawChunk("world", DOOR.x() >> 4, DOOR.z() >> 4);

        assertEquals(1, signs.of("hovel_1").size(), "a sign in another chunk is not checked");
    }

    @Test
    void deletingTheClaimReleasesItsSigns() {
        signs.link("house", DOOR);

        store.delete("house", ChangeCause.COMMAND);

        assertTrue(blocks.released(DOOR));
        assertTrue(signs.at(DOOR).isEmpty());
    }
}
