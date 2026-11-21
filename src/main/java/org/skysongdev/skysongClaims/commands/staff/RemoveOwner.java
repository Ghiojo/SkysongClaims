package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaims removeowner <id> <player> [profile]}: staff remove an owner, which owners
 * themselves can never do to each other. Without a profile, the player's only owning character.
 */
final class RemoveOwner extends LeafCommand {
    private final StaffSupport support;
    private final OwnershipService ownership;

    RemoveOwner(StaffSupport support, OwnershipService ownership) {
        super(support.messenger(), "removeowner", "<id> <player> [profile]", 2, null);
        this.support = support;
        this.ownership = ownership;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Claim claim = support.claims().require(args[0]);
        CharacterId character = args.length > 2
                ? support.characterArguments().character(args[1], args[2])
                : onlyOwnerOf(claim, args[1]);
        ownership.removeOwner(claim.id(), character, ChangeCause.COMMAND);
        messenger.send(sender, "claims.owner-removed", unparsed("claim", claim.id()),
                unparsed("character", ClaimArguments.describe(character)));
    }

    /** The player's character that owns the Claim; asks for the profile when there are several. */
    private static CharacterId onlyOwnerOf(Claim claim, String playerName) {
        List<CharacterId> owning = claim.owners().stream().map(ClaimOwner::character)
                .filter(character -> playerNameOf(character).equalsIgnoreCase(playerName))
                .toList();
        if (owning.size() == 1) {
            return owning.get(0);
        }
        if (owning.isEmpty()) {
            throw RefusedException.of("claims.player-not-owner", "claim", claim.id(), "player", playerName);
        }
        throw RefusedException.of("claims.which-profile", "player", playerName);
    }

    private static String playerNameOf(CharacterId character) {
        String name = org.bukkit.Bukkit.getOfflinePlayer(character.player()).getName();
        return name == null ? "" : name;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> support.state().find(args[0]).map(claim -> ClaimArguments.startingWith(claim.owners().stream()
                    .map(owner -> playerNameOf(owner.character())).filter(name -> !name.isEmpty()).distinct(), args[1]))
                    .orElse(List.of());
            case 3 -> support.characterArguments().profiles(args[1], args[2]);
            default -> List.of();
        };
    }
}
