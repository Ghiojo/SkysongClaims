package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.ownership.InviteService;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaim accept [id]} and {@code /sclaim deny [id]}: answers a co-owner invite for the character you play. */
final class AnswerInvite extends LeafCommand {
    private final PlayerSupport support;
    private final boolean accept;

    AnswerInvite(PlayerSupport support, boolean accept) {
        super(support.messenger(), accept ? "accept" : "deny", "[id]", 0, null);
        this.support = support;
        this.accept = accept;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        CharacterId character = support.characters().own(sender);
        Optional<String> claimId = args.length > 0 ? Optional.of(args[0]) : Optional.empty();
        if (accept) {
            Claim claim = support.invites().accept(character, claimId);
            messenger.send(sender, "player.accepted", unparsed("claim", claim.id()), parsed("name", claim.displayName()));
        } else {
            InviteService.Invite invite = support.invites().deny(character, claimId);
            messenger.send(sender, "player.denied", unparsed("claim", invite.claimId()));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        try {
            CharacterId character = support.characters().own(sender);
            return ClaimArguments.startingWith(support.invites().pending(character).stream().map(InviteService.Invite::claimId),
                    args[0]);
        } catch (RuntimeException noCharacter) {
            return List.of();
        }
    }
}
