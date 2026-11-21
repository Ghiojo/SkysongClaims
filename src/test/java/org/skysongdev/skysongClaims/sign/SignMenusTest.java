package org.skysongdev.skysongClaims.sign;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.MenuButton;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.MutableClock;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.support.TestTypes;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignMenusTest {
    private PlayerMock player;
    private CharacterId alice;
    private ClaimStore store;
    private MenuButtons buttons;
    private SignMenus menus;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        player = server.addPlayer("Alice");
        FakeCharacters characters = new FakeCharacters();
        alice = characters.play(player.getUniqueId(), "Alice");
        ClaimState state = new ClaimState();
        store = new ClaimStore(state, new InMemoryClaimRepository());
        store.save(Claim.create("hovel_1", "hovel", OptionalInt.of(2), List.of(new ClaimRegion("world", "hovel_1"))), ChangeCause.COMMAND);
        OwnershipService ownership = new OwnershipService(store, TestTypes::registry,
                new Limits(state, new LimitRules((what, e) -> {
                })), characters, new MutableClock());
        buttons = new MenuButtons();
        menus = new SignMenus(state, ownership, characters, TestTypes::registry, buttons, TestMessages.messenger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ClaimMenu menu() {
        return menus.build(player, store.require("hovel_1"), false);
    }

    @Test
    void showsInfoAndTakeSlotOnAFreeClaimWithRoom() {
        ClaimMenu menu = menu();

        assertEquals(Material.OAK_SIGN, menu.getInventory().getItem(11).getType());
        assertEquals(Material.LIME_DYE, menu.getInventory().getItem(13).getType());
        assertNull(menu.getInventory().getItem(15), "no Leave for someone who doesn't own it");
    }

    @Test
    void takeSlotMakesYouAnOwner() {
        menu().click(13, player);

        assertTrue(store.require("hovel_1").isOwnedBy(alice));
    }

    @Test
    void ownersLeaveWithTwoClicks() {
        menu().click(13, player);
        ClaimMenu owned = menu();
        assertNull(owned.getInventory().getItem(13), "no Take slot for an owner");
        assertEquals(Material.BARRIER, owned.getInventory().getItem(15).getType());

        owned.click(15, player);
        assertTrue(store.require("hovel_1").isOwnedBy(alice), "the first click only asks");

        ClaimMenu confirm = (ClaimMenu) player.getOpenInventory().getTopInventory().getHolder();
        confirm.click(15, player);
        assertFalse(store.require("hovel_1").isOwned());
    }

    @Test
    void otherPluginsAddButtonsToTheBottomRow() {
        List<String> clicked = new ArrayList<>();
        buttons.add("SkysongEconomy", "buy", new MenuButton() {
            @Override
            public Optional<ItemStack> icon(Player viewer, ClaimInfo claim) {
                return Optional.of(new ItemStack(Material.EMERALD));
            }

            @Override
            public void click(Player viewer, ClaimInfo claim) {
                clicked.add(claim.id());
            }
        });
        buttons.add("Other", "hidden", new MenuButton() {
            @Override
            public Optional<ItemStack> icon(Player viewer, ClaimInfo claim) {
                return Optional.empty();
            }

            @Override
            public void click(Player viewer, ClaimInfo claim) {
            }
        });

        ClaimMenu menu = menu();
        assertEquals(Material.EMERALD, menu.getInventory().getItem(18).getType());
        assertNull(menu.getInventory().getItem(19));

        menu.click(18, player);
        assertEquals(List.of("hovel_1"), clicked);

        buttons.removeAll("SkysongEconomy");
        assertNull(menu().getInventory().getItem(18));
    }
}
