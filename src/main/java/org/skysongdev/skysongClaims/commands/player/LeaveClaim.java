package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaim leave [id] [confirm]}: the character you play stops owning a Claim, by default
 * the one you stand in. It asks for {@code confirm} first, since a bought Claim can't be taken back.
 */
final class LeaveClaim extends LeafCommand {
    private static final String CONFIRM = "confirm";
    private final PlayerSupport support;

    LeaveClaim(PlayerSupport support) {
        super(support.messenger(), "leave", "[id] [confirm]", 0, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        boolean confirmed = args.length > 0 && args[args.length - 1].equalsIgnoreCase(CONFIRM);
        int idArgs = confirmed ? args.length - 1 : args.length;
        CharacterId character = support.characters().own(sender);
        Claim claim = support.claimOrHere(sender, idArgs > 0 ? Optional.of(args[0]) : Optional.empty());
        if (!claim.isOwnedBy(character)) {
            throw RefusedException.of("ownership.not-owner", "claim", claim.id());
        }
        if (!confirmed) {
            messenger.send(sender, claim.owners().size() == 1 ? "player.leave-confirm-last" : "player.leave-confirm",
                    unparsed("claim", claim.id()), parsed("name", claim.displayName()));
            return;
        }
        support.ownership().removeOwner(claim.id(), character, ChangeCause.LEAVE);
        messenger.send(sender, "player.left", unparsed("claim", claim.id()), parsed("name", claim.displayName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.ownIds(sender, args[0]);
            case 2 -> List.of(CONFIRM);
            default -> List.of();
        };
    }
}
