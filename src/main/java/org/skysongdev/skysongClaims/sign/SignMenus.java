package org.skysongdev.skysongClaims.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.MenuButton;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.character.CharacterDirectory;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * Builds the menu a Claim sign opens: the Claim's name, type, owners and open slots, a Take slot
 * button on a Free Claim with room, a Leave button for its owners, and the buttons other
 * plugins added in the bottom row. Groups stay commands (decision F9).
 */
public final class SignMenus {
    private static final int INFO_SLOT = 11;
    private static final int TAKE_SLOT = 13;
    private static final int LEAVE_SLOT = 15;
    private static final int FIRST_PLUGIN_SLOT = 18;

    private final ClaimState claims;
    private final OwnershipService ownership;
    private final CharacterDirectory characters;
    private final Supplier<ClaimTypeRegistry> types;
    private final MenuButtons buttons;
    private final Messenger messenger;

    public SignMenus(ClaimState claims, OwnershipService ownership, CharacterDirectory characters,
                     Supplier<ClaimTypeRegistry> types, MenuButtons buttons, Messenger messenger) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.characters = Objects.requireNonNull(characters, "characters");
        this.types = Objects.requireNonNull(types, "types");
        this.buttons = Objects.requireNonNull(buttons, "buttons");
        this.messenger = Objects.requireNonNull(messenger, "messenger");
    }

    /** Opens the Claim's menu for the player. */
    public void open(Player player, Claim claim) {
        player.openInventory(build(player, claim, false).getInventory());
    }

    ClaimMenu build(Player player, Claim claim, boolean confirmLeave) {
        ClaimMenu menu = new ClaimMenu(claim.id(), messenger.render("signs.menu.title", parsed("name", claim.displayName())));
        menu.set(INFO_SLOT, info(claim), null);
        Optional<CharacterId> character = characters.active(player.getUniqueId());
        if (claim.free() && claim.hasRoom() && character.filter(claim::isOwnedBy).isEmpty()) {
            menu.set(TAKE_SLOT, item(Material.LIME_DYE, "signs.menu.take", List.of()), viewer -> take(viewer, claim.id()));
        }
        if (character.filter(claim::isOwnedBy).isPresent()) {
            if (confirmLeave) {
                menu.set(LEAVE_SLOT, item(Material.RED_CONCRETE, "signs.menu.leave-confirm", List.of()),
                        viewer -> leave(viewer, claim.id()));
            } else {
                menu.set(LEAVE_SLOT, item(Material.BARRIER, "signs.menu.leave", List.of()),
                        viewer -> viewer.openInventory(build(viewer, claims.find(claim.id()).orElse(claim), true).getInventory()));
            }
        }
        int slot = FIRST_PLUGIN_SLOT;
        for (MenuButton button : buttons.all()) {
            if (slot >= ClaimMenu.SIZE) {
                break;
            }
            Optional<ItemStack> icon = button.icon(player, claim.info());
            if (icon.isPresent()) {
                menu.set(slot++, icon.get(), viewer -> {
                    viewer.closeInventory();
                    claims.find(claim.id()).ifPresent(current -> button.click(viewer, current.info()));
                });
            }
        }
        return menu;
    }

    private ItemStack info(Claim claim) {
        String typeName = types.get().find(claim.typeId()).map(ClaimType::displayName).orElse(claim.typeId());
        String owners = claim.owners().stream().map(ClaimOwner::character).map(ClaimArguments::describe)
                .collect(Collectors.joining(", "));
        List<Component> lore = new ArrayList<>(List.of(
                messenger.render("signs.menu.info-type", parsed("type_name", typeName)),
                messenger.render(claim.isOwned() ? "signs.menu.info-owners" : "signs.menu.info-no-owners",
                        unparsed("owners", owners))));
        if (claim.free()) {
            lore.add(messenger.render("signs.menu.info-slots",
                    unparsed("open", Integer.toString(claim.openSlots().orElseThrow())),
                    unparsed("slots", Integer.toString(claim.slots().orElseThrow()))));
        }
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(messenger.render("signs.menu.info-name", parsed("name", claim.displayName()))));
        meta.lore(lore.stream().map(SignMenus::plain).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(Material material, String nameKey, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(messenger.render(nameKey)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Item names are italic unless told otherwise. */
    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private void take(Player player, String claimId) {
        player.closeInventory();
        try {
            CharacterId character = characters.active(player.getUniqueId())
                    .orElseThrow(() -> RefusedException.of("command.you-have-no-character"));
            Claim claim = ownership.take(character, claimId);
            messenger.send(player, "player.taken", unparsed("claim", claim.id()), parsed("name", claim.displayName()),
                    unparsed("open", Integer.toString(claim.openSlots().orElseThrow())));
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }

    private void leave(Player player, String claimId) {
        player.closeInventory();
        try {
            CharacterId character = characters.active(player.getUniqueId())
                    .orElseThrow(() -> RefusedException.of("command.you-have-no-character"));
            Claim claim = ownership.removeOwner(claimId, character, ChangeCause.LEAVE);
            messenger.send(player, "player.left", unparsed("claim", claim.id()), parsed("name", claim.displayName()));
        } catch (RefusedException refusal) {
            messenger.send(player, refusal);
        }
    }
}
