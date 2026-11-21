package org.skysongdev.skysongClaims.apiimpl;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.LimitSubject;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.OwnershipCheck;
import org.skysongdev.skysongClaims.api.SignInfo;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.sign.SignLocation;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkysongClaimsApiImplTest {
    private static final SignLocation BOARD = new SignLocation("world", 1, 64, 1);

    private ApiFixture fixture;
    private SkysongClaimsApiImpl api;
    private Plugin economy;
    private CharacterId alice;
    private CharacterId bob;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        economy = MockBukkit.createMockPlugin("SkysongEconomy");
        fixture = new ApiFixture();
        api = fixture.api;
        alice = fixture.characters.add(UUID.randomUUID(), "Alice");
        bob = fixture.characters.add(UUID.randomUUID(), "Bob");
        server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void readsClaimsAsSnapshots() {
        assertEquals(List.of("house", "hovel_1"), api.claims().all().stream().map(ClaimInfo::id).sorted().toList());
        ClaimInfo hovel = api.claims().find("hovel_1").orElseThrow();
        assertTrue(hovel.free());
        assertEquals(OptionalInt.of(2), hovel.openSlots());
        assertEquals(Optional.of("house"), api.claims().ofRegion("world", "house").map(ClaimInfo::id));
        assertTrue(api.claims().find("castle").isEmpty());
    }

    @Test
    void findsTheClaimAtAPlace() {
        fixture.regions.standingIn(new ClaimRegion("world", "house"));

        Location somewhere = new Location(MockBukkit.getMock().getWorld("world"), 0, 64, 0);
        assertEquals(Optional.of("house"), api.claims().at(somewhere).map(ClaimInfo::id));
    }

    @Test
    void readsTypes() {
        assertEquals(List.of("hovel", "other"), api.types().all().stream().map(type -> type.id()).toList());
        assertTrue(api.types().find("hovel").orElseThrow().freeClaim());
    }

    @Test
    void refusesOffTheMainThread() {
        fixture.mainThread = false;

        assertThrows(IllegalStateException.class, () -> api.claims().all());
        assertThrows(IllegalStateException.class, () -> api.ownership().addOwner(economy, "house", alice));
    }

    @Test
    void anUnknownClaimIsTheCallersMistake() {
        assertThrows(IllegalArgumentException.class, () -> api.ownership().owners("castle"));
        assertThrows(IllegalArgumentException.class, () -> api.market().setForSale(economy, "castle", true));
        assertThrows(IllegalArgumentException.class, () -> api.limits().limit(LimitSubject.ofCharacter(alice), "manor"));
    }

    @Test
    void addsAnOwnerWhenAllowed() {
        assertEquals(OwnershipCheck.OK, api.ownership().canOwn(alice, "house"));
        assertEquals(OwnershipCheck.OK, api.ownership().addOwner(economy, "house", alice));

        assertTrue(api.ownership().isOwner("house", alice));
        assertEquals(OwnerCause.API, api.ownership().owners("house").getFirst().cause());
        assertEquals(List.of("house"), api.claims().ownedBy(alice).stream().map(ClaimInfo::id).toList());
    }

    @Test
    void answersWhyItCantAddAnOwner() {
        api.ownership().addOwner(economy, "house", alice);

        assertEquals(OwnershipCheck.ALREADY_OWNER, api.ownership().addOwner(economy, "house", alice));
        api.ownership().addOwner(economy, "hovel_1", alice);
        api.ownership().addOwner(economy, "hovel_1", bob);
        CharacterId carol = fixture.characters.add(UUID.randomUUID(), "Carol");
        assertEquals(OwnershipCheck.FULL, api.ownership().addOwner(economy, "hovel_1", carol));
        assertEquals(OwnershipCheck.OK, api.ownership().addOwner(economy, "house", bob), "a bought Claim has no cap");
        assertEquals(OwnershipCheck.UNKNOWN_CHARACTER,
                api.ownership().addOwner(economy, "hovel_1", new CharacterId(UUID.randomUUID(), "Ghost")));
        assertEquals(List.of(alice, bob), api.ownership().owners("house").stream().map(owner -> owner.character()).toList());
    }

    @Test
    void removesOwners() {
        api.ownership().addOwner(economy, "hovel_1", alice);
        api.ownership().addOwner(economy, "hovel_1", bob);

        assertFalse(api.ownership().removeOwner(economy, "house", alice));
        assertTrue(api.ownership().removeOwner(economy, "hovel_1", alice));
        assertFalse(api.ownership().isOwner("hovel_1", alice));

        api.ownership().clearOwners(economy, "hovel_1");
        assertTrue(api.ownership().owners("hovel_1").isEmpty());
    }

    @Test
    void transfersAnOwnersPlaceKeepingGroups() {
        CharacterId carol = fixture.characters.add(UUID.randomUUID(), "Carol");
        api.ownership().addOwner(economy, "house", alice);
        fixture.store.save(fixture.store.require("house").withGroups(List.of(
                new ClaimGroup("friends", "Friends", EnumSet.of(GroupPermission.BUILD), Set.of(bob, carol)))),
                ChangeCause.COMMAND);

        assertEquals(OwnershipCheck.OK, api.ownership().transfer(economy, "house", alice, bob));

        assertEquals(List.of(bob), api.ownership().owners("house").stream().map(owner -> owner.character()).toList());
        assertEquals(Set.of(carol), api.groups().of("house").getFirst().members());
    }

    @Test
    void refusesATransferFromSomeoneWhoIsntAnOwner() {
        assertEquals(OwnershipCheck.UNKNOWN_CHARACTER, api.ownership().transfer(economy, "house", alice, bob));
        assertTrue(api.ownership().owners("house").isEmpty());
    }

    @Test
    void refusesATransferOverTheTypeLimit() {
        // Bob already has his one hovel, so he can't take Alice's place in another.
        fixture.store.save(fixture.store.require("house").withType("hovel"), ChangeCause.COMMAND);
        api.ownership().addOwner(economy, "hovel_1", bob);
        api.ownership().addOwner(economy, "house", alice);

        assertEquals(OwnershipCheck.TYPE_LIMIT, api.ownership().transfer(economy, "house", alice, bob));
        assertTrue(api.ownership().isOwner("house", alice));
    }

    @Test
    void putsClaimsOnSale() {
        api.market().setForSale(economy, "house", true);

        assertTrue(api.market().isForSale("house"));
        assertEquals(Optional.of("SkysongEconomy"), api.claims().find("house").orElseThrow().forSaleBy());
        assertEquals(List.of("house"), api.market().forSale().stream().map(ClaimInfo::id).toList());

        api.market().setForSale(economy, "house", false);
        assertFalse(api.market().isForSale("house"));
    }

    @Test
    void ownersAndBuildGroupMembersCanBuild() {
        CharacterId carol = fixture.characters.add(UUID.randomUUID(), "Carol");
        api.ownership().addOwner(economy, "house", alice);
        fixture.store.save(fixture.store.require("house").withGroups(List.of(
                new ClaimGroup("friends", "Friends", EnumSet.of(GroupPermission.BUILD), Set.of(bob)))),
                ChangeCause.COMMAND);

        assertTrue(api.groups().canBuild(alice, "house"));
        assertTrue(api.groups().canBuild(bob, "house"));
        assertFalse(api.groups().canBuild(carol, "house"));
    }

    @Test
    void liveRulesRaiseLimitsUntilTheirPluginDisables() {
        LimitSubject subject = api.limits().subject(alice, "hovel");
        assertEquals(1, api.limits().limit(subject, "hovel"));

        api.limits().registerRule(economy, "noble", (who, type) -> type.id().equals("hovel") ? 1 : 0);
        assertEquals(2, api.limits().limit(subject, "hovel"));
        api.ownership().addOwner(economy, "hovel_1", alice);
        assertEquals(1, api.limits().count(subject, "hovel"));
        assertEquals(1, api.limits().remaining(subject, "hovel"));

        new PluginCleanup(fixture.limitRules, fixture.menuButtons)
                .onPluginDisable(new org.bukkit.event.server.PluginDisableEvent(economy));
        assertEquals(1, api.limits().limit(subject, "hovel"));
    }

    @Test
    void setsThePriceLineOnEverySignOfTheClaim() {
        fixture.blocks.sign(BOARD);
        fixture.signs.link("house", BOARD);

        assertEquals(List.of(new SignInfo("house", "world", 1, 64, 1)), api.signs().of("house"));
        api.signs().setPriceLine(economy, "house", Component.text("500 crowns"));
        assertEquals("500 crowns", fixture.blocks.lines(BOARD).get(3));

        api.signs().clearPriceLine(economy, "house");
        assertEquals("", fixture.blocks.lines(BOARD).get(3));
    }

    @Test
    void menuButtonsGoAwayWithTheirPlugin() {
        api.signs().addMenuButton(economy, "buy", new org.skysongdev.skysongClaims.api.MenuButton() {
            @Override
            public Optional<org.bukkit.inventory.ItemStack> icon(org.bukkit.entity.Player viewer, ClaimInfo claim) {
                return Optional.empty();
            }

            @Override
            public void click(org.bukkit.entity.Player viewer, ClaimInfo claim) {
            }
        });
        assertEquals(1, fixture.menuButtons.all().size());

        new PluginCleanup(fixture.limitRules, fixture.menuButtons)
                .onPluginDisable(new org.bukkit.event.server.PluginDisableEvent(economy));
        assertTrue(fixture.menuButtons.all().isEmpty());
    }
}
