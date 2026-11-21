package org.skysongdev.skysongClaims.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongClaims.claim.ClaimCheck;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.ClaimService;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.commands.staff.StaffCommand;
import org.skysongdev.skysongClaims.commands.staff.StaffSupport;
import org.skysongdev.skysongClaims.config.ConfigException;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;

import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffCommandTest {
    private ServerMock server;
    private PlayerMock staff;
    private ConsoleCommandSenderMock console;
    private ClaimState state;
    private StaffCommand command;
    private boolean reloadFails;
    private CharacterId alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        staff = server.addPlayer("Staff");
        console = (ConsoleCommandSenderMock) server.getConsoleSender();
        state = new ClaimState();
        Supplier<ClaimTypeRegistry> types = TestTypes::registry;
        FakeRegions regions = new FakeRegions().add("world", "hovel_1", "hovel_2").add("world_nether", "cellar");
        ClaimStore store = new ClaimStore(state, new InMemoryClaimRepository());
        ClaimService claims = new ClaimService(store, types, regions);
        Messenger messenger = TestMessages.messenger();
        FakeCharacters characters = new FakeCharacters();
        OwnershipService ownership = new OwnershipService(store, types, new Limits(state, new LimitRules((w, e) -> {
        })), characters, Clock.systemUTC());
        alice = characters.play(server.addPlayer("Alice").getUniqueId(), "Alice");
        command = new StaffCommand(new StaffSupport(claims, state, types, new ClaimArguments(state, types, regions),
                new CharacterArguments(characters), messenger), ownership,
                () -> {
                    if (reloadFails) {
                        throw new ConfigException("types.yml: broken");
                    }
                }, () -> new ClaimCheck.Report(List.of(new ClaimCheck.Missing("a", new ClaimRegion("world", "gone"))), List.of()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static List<String> messages(org.bukkit.command.CommandSender sender) {
        List<String> lines = new ArrayList<>();
        Component next;
        while ((next = nextMessage(sender)) != null) {
            lines.add(PlainTextComponentSerializer.plainText().serialize(next));
        }
        return lines;
    }

    private static Component nextMessage(org.bukkit.command.CommandSender sender) {
        if (sender instanceof PlayerMock player) {
            return player.nextComponentMessage();
        }
        return ((ConsoleCommandSenderMock) sender).nextComponentMessage();
    }

    private List<String> run(org.bukkit.command.CommandSender sender, String line) {
        command.execute(sender, line.split(" "));
        return messages(sender);
    }

    @Test
    void createUsesTheStaffMembersWorldUnlessOneIsGiven() {
        List<String> said = run(staff, "create hovel_1 hovel hovel_1 world_nether:cellar");

        assertTrue(said.get(0).contains("Made the Free Claim hovel_1"), said.toString());
        assertEquals(List.of(new ClaimRegion("world", "hovel_1"), new ClaimRegion("world_nether", "cellar")),
                state.find("hovel_1").orElseThrow().regions());
    }

    @Test
    void theConsoleMustNameTheWorld() {
        List<String> said = run(console, "create a hovel hovel_1");

        assertTrue(said.get(0).contains("world:hovel_1"), said.toString());
        assertTrue(state.all().isEmpty());
    }

    @Test
    void refusalsAreExplained() {
        assertTrue(run(staff, "create a manor hovel_1").get(0).contains("no Claim type called manor"));
        assertTrue(run(staff, "info nothing").get(0).contains("no Claim called nothing"));
        assertTrue(run(staff, "create a").get(0).contains("Usage: /create"));
        assertTrue(run(staff, "fly").get(0).contains("Unknown command fly"));
    }

    @Test
    void deleteAsksFirst() {
        run(staff, "create a other hovel_1");

        assertTrue(run(staff, "delete a").get(0).contains("confirm"));
        assertTrue(state.find("a").isPresent());

        assertTrue(run(staff, "delete a confirm").get(0).contains("Deleted"));
        assertTrue(state.find("a").isEmpty());
    }

    @Test
    void infoShowsTheClaim() {
        run(staff, "create a hovel hovel_1");
        run(staff, "rename a <gold>Rose Cottage");

        List<String> said = run(staff, "info a");

        assertTrue(said.get(0).contains("Rose Cottage"), said.toString());
        assertTrue(said.stream().anyMatch(line -> line.contains("2 of 2 slot(s) open")), said.toString());
        assertTrue(said.stream().anyMatch(line -> line.contains("world:hovel_1")), said.toString());
        assertTrue(said.stream().anyMatch(line -> line.contains("nobody")), said.toString());
    }

    @Test
    void freeAndMaxOwnersChangeSlots() {
        run(staff, "create a other hovel_1");

        assertTrue(run(staff, "free a true 3").get(0).contains("Free Claim with 3 slot(s)"));
        assertTrue(run(staff, "maxowners a 4").get(0).contains("room for 4"));
        assertTrue(run(staff, "maxowners a lots").get(0).contains("not a whole number"));
        assertTrue(run(staff, "free a maybe").get(0).contains("true or false"));
    }

    @Test
    void listFiltersByType() {
        run(staff, "create a other hovel_1");
        run(staff, "create b hovel hovel_2");

        List<String> said = run(staff, "list hovel");

        assertEquals(2, said.size(), said.toString());
        assertTrue(said.get(1).contains("b"));
        assertTrue(run(staff, "list castle").get(0).contains("No Claims"));
    }

    @Test
    void regionsChange() {
        run(staff, "create a other hovel_1");

        assertTrue(run(staff, "addregion a hovel_2").get(0).contains("Added world:hovel_2"));
        assertTrue(run(staff, "removeregion a world:hovel_1").get(0).contains("Removed world:hovel_1"));
        assertTrue(run(staff, "settype a hovel").get(0).contains("now a hovel"));
    }

    @Test
    void aBrokenReloadSaysWhy() {
        assertTrue(run(staff, "reload").get(0).contains("Reloaded SkysongClaims: 2"));

        reloadFails = true;

        assertTrue(run(staff, "reload").get(0).contains("types.yml: broken"));
    }

    @Test
    void staffAddAndRemoveOwners() {
        run(staff, "create a other hovel_1");

        assertTrue(run(staff, "addowner a Alice").get(0).contains("Alice now owns a"));
        assertTrue(state.find("a").orElseThrow().isOwnedBy(alice));
        assertTrue(run(staff, "addowner a Alice").get(0).contains("already owns"));

        assertTrue(run(staff, "removeowner a Alice").get(0).contains("Alice no longer owns a"));
        assertTrue(state.find("a").orElseThrow().owners().isEmpty());
        assertTrue(run(staff, "removeowner a Alice").get(0).contains("No character of Alice"));
        assertTrue(run(staff, "addowner a Nobody").get(0).contains("no player called Nobody"));
    }

    @Test
    void forceSkipsTheLimit() {
        run(staff, "create a hovel hovel_1");
        run(staff, "create b hovel hovel_2");
        run(staff, "addowner a Alice");

        assertTrue(run(staff, "addowner b Alice").get(0).contains("as many hovel Claims as allowed"));
        assertTrue(run(staff, "addowner b Alice -force").get(0).contains("Alice now owns b"));
    }

    @Test
    void checkReportsWhatItFound() {
        List<String> said = run(staff, "check");

        assertTrue(said.get(0).contains("world:gone"), said.toString());
        assertTrue(said.get(1).contains("1 missing region(s), 0 character(s) released"), said.toString());
    }

    @Test
    void completesSubcommandsTypesAndRegions() {
        run(staff, "create a other hovel_1");

        assertTrue(command.tabComplete(staff, new String[]{"cr"}).contains("create"));
        assertEquals(List.of("hovel"), command.tabComplete(staff, new String[]{"create", "x", "h"}));
        assertEquals(List.of("hovel_2"), command.tabComplete(staff, new String[]{"create", "x", "hovel", "hovel_2"}));
        assertEquals(List.of("world_nether:cellar"),
                command.tabComplete(staff, new String[]{"addregion", "a", "world_nether:"}));
        assertEquals(List.of("a"), command.tabComplete(staff, new String[]{"info", ""}));
    }
}
