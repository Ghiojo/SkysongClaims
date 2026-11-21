package org.skysongdev.skysongClaims.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimLocator;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.commands.player.PlayerCommand;
import org.skysongdev.skysongClaims.commands.player.PlayerSupport;
import org.skysongdev.skysongClaims.flag.FlagService;
import org.skysongdev.skysongClaims.group.GroupLimits;
import org.skysongdev.skysongClaims.group.GroupService;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.ownership.InviteService;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongClaims.support.MutableClock;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCommandTest {
    private static final ClaimRegion HOVEL = new ClaimRegion("world", "hovel_1");

    private PlayerMock alicePlayer;
    private PlayerMock bobPlayer;
    private CharacterId alice;
    private CharacterId bob;
    private ClaimStore store;
    private FakeRegions regions;
    private FakeCharacters characters;
    private PlayerCommand command;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        server.addSimpleWorld("world");
        alicePlayer = server.addPlayer("Alice");
        bobPlayer = server.addPlayer("Bob");
        characters = new FakeCharacters();
        alice = characters.play(alicePlayer.getUniqueId(), "Alice");
        bob = characters.play(bobPlayer.getUniqueId(), "Bob");
        ClaimState state = new ClaimState();
        store = new ClaimStore(state, new InMemoryClaimRepository());
        store.save(Claim.create("hovel_1", "hovel", OptionalInt.of(2), List.of(HOVEL)).withDisplayName("<gold>Rose Hovel"),
                ChangeCause.COMMAND);
        store.save(Claim.create("house", "other", OptionalInt.empty(), List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);
        regions = new FakeRegions().add("world", "hovel_1", "house").standingIn(HOVEL);
        MutableClock clock = new MutableClock();
        Limits limits = new Limits(state, new LimitRules((what, e) -> {
        }));
        OwnershipService ownership = new OwnershipService(store, TestTypes::registry, limits, characters, clock);
        InviteService invites = new InviteService(store, ownership, () -> Duration.ofMinutes(5), clock);
        command = new PlayerCommand(new PlayerSupport(state, new ClaimLocator(state, regions), ownership, invites, limits,
                TestTypes::registry, new CharacterArguments(characters), TestMessages.messenger()),
                new GroupService(store, () -> new GroupLimits(5, 0)),
                new FlagService(store, () -> List.of("greeting", "farewell", "use"), TestTypes::registry, regions));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static List<String> messages(PlayerMock player) {
        List<String> lines = new ArrayList<>();
        Component next;
        while ((next = player.nextComponentMessage()) != null) {
            lines.add(PlainTextComponentSerializer.plainText().serialize(next));
        }
        return lines;
    }

    private List<String> run(PlayerMock player, String line) {
        command.execute(player, line.split(" "));
        return messages(player);
    }

    @Test
    void takeMovesYouIntoTheFreeClaimYouStandIn() {
        assertTrue(run(alicePlayer, "take").get(0).contains("You now live in Rose Hovel. 1 slot(s) left."));
        assertTrue(store.require("hovel_1").isOwnedBy(alice));

        assertTrue(run(alicePlayer, "take").get(0).contains("already owns"));
    }

    @Test
    void takeOutsideAClaimSaysSo() {
        regions.standingIn();

        assertTrue(run(alicePlayer, "take").get(0).contains("aren't standing in a Claim"));
    }

    @Test
    void youNeedACharacter() {
        characters.delete(alice);

        assertTrue(run(alicePlayer, "take").get(0).contains("playing a character first"));
    }

    @Test
    void infoShowsTheClaimYouStandIn() {
        run(alicePlayer, "take");

        List<String> said = run(bobPlayer, "info");

        assertTrue(said.get(0).contains("Rose Hovel"), said.toString());
        assertTrue(said.get(1).contains("Alice"), said.toString());
        assertTrue(said.get(2).contains("1 of 2 slot(s) open"), said.toString());
    }

    @Test
    void inviteAcceptAndLeave() {
        run(alicePlayer, "take");

        assertTrue(run(alicePlayer, "invite hovel_1 Bob").get(0).contains("Invited Bob"));
        assertTrue(messages(bobPlayer).get(0).contains("/sclaim accept hovel_1"));
        assertTrue(run(bobPlayer, "accept").get(0).contains("You now co-own Rose Hovel"));
        assertTrue(store.require("hovel_1").isOwnedBy(bob));

        assertTrue(run(bobPlayer, "leave hovel_1").get(0).contains("The other owners keep it"));
        assertTrue(store.require("hovel_1").isOwnedBy(bob), "nothing happens before confirm");
        assertTrue(run(bobPlayer, "leave hovel_1 confirm").get(0).contains("no longer own"));
        assertFalse(store.require("hovel_1").isOwnedBy(bob));
    }

    @Test
    void leaveDefaultsToTheClaimYouStandInAndWarnsTheLastOwner() {
        run(alicePlayer, "take");

        assertTrue(run(alicePlayer, "leave").get(0).contains("last owner"));
        run(alicePlayer, "leave confirm");

        assertFalse(store.require("hovel_1").isOwned());
    }

    @Test
    void ownersCantRemoveEachOther() {
        run(alicePlayer, "take");
        run(bobPlayer, "take");

        assertTrue(run(alicePlayer, "fly").get(0).contains("Unknown command"));
        assertTrue(command.tabComplete(alicePlayer, new String[]{""}).stream().noneMatch(name -> name.contains("remove")));
    }

    @Test
    void denyDropsTheInvite() {
        run(alicePlayer, "take");
        run(alicePlayer, "invite hovel_1 Bob");
        messages(bobPlayer);

        assertTrue(run(bobPlayer, "deny").get(0).contains("turned down"));
        assertTrue(run(bobPlayer, "accept").get(0).contains("no Claim invite"));
    }

    @Test
    void listShowsYourClaimsAndLimits() {
        run(alicePlayer, "take");

        List<String> said = run(alicePlayer, "list");

        assertTrue(said.get(0).contains("owns 1 Claim(s)"), said.toString());
        assertTrue(said.get(1).contains("Rose Hovel"), said.toString());
        assertEquals("Hovel: 1 of 1 for this character", said.get(2).substring(said.get(2).indexOf("Hovel")), said.toString());
        assertTrue(said.get(3).contains("Other: 0 of 3"), said.toString());
    }

    @Test
    void ownersManageBuildGroups() {
        run(alicePlayer, "take");

        assertTrue(run(alicePlayer, "group hovel_1 create Family").get(0).contains("Made the group family"));
        assertTrue(run(alicePlayer, "group hovel_1 add family Bob").get(0).contains("Bob can now build"));
        assertTrue(store.require("hovel_1").group("family").orElseThrow().members().contains(bob));
        assertTrue(run(alicePlayer, "group hovel_1 list").get(0).contains("Family (family): Bob"));
        assertTrue(run(alicePlayer, "group hovel_1 remove family Bob").get(0).contains("Bob is no longer in family"));
        assertTrue(run(alicePlayer, "group hovel_1 add family Alice").get(0).contains("already build"));
        assertTrue(run(alicePlayer, "group hovel_1 delete family").get(0).contains("Deleted the group family"));
    }

    @Test
    void onlyOwnersManageGroups() {
        assertTrue(run(bobPlayer, "group hovel_1 create Family").get(0).contains("You don't own hovel_1"));
        assertTrue(run(bobPlayer, "group hovel_1 fly").get(0).contains("Usage"));
    }

    @Test
    void ownersSetGreetingButNeverOpenUseFlags() {
        run(alicePlayer, "take");

        assertTrue(run(alicePlayer, "flag hovel_1 greeting Welcome home").get(0).contains("Set greeting on hovel_1 to Welcome home"));
        assertEquals("Welcome home", store.require("hovel_1").flags().get("greeting"));
        assertTrue(run(alicePlayer, "flag hovel_1 use deny").get(0).contains("can't set use"));
        assertTrue(run(alicePlayer, "flag hovel_1 greeting clear").get(0).contains("Cleared greeting"));
        assertTrue(store.require("hovel_1").flags().isEmpty());
        assertTrue(run(bobPlayer, "flag hovel_1 greeting Hi").get(0).contains("You don't own hovel_1"));
    }
}
