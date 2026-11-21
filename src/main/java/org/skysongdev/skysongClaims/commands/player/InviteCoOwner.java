package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.ownership.InviteService;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaim invite <id> <player> [profile]}: an owner invites a character to co-own the
 * Claim; it is the one the player plays now unless a profile is given.
 */
final class InviteCoOwner extends LeafCommand {
    private final PlayerSupport support;

    InviteCoOwner(PlayerSupport support) {
        super(support.messenger(), "invite", "<id> <player> [profile]", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        CharacterId inviter = support.characters().own(sender);
        CharacterId invitee = support.characters().character(args[1], args.length > 2 ? args[2] : null);
        InviteService.Invite invite = support.invites().invite(inviter, args[0], invitee);
        Claim claim = support.state().find(invite.claimId()).orElseThrow();
        String minutes = Long.toString(Math.max(1, support.invites().expiry().toMinutes()));
        messenger.send(sender, "player.invited", unparsed("claim", claim.id()),
                unparsed("character", ClaimArguments.describe(invitee)));
        Player target = Bukkit.getPlayer(invitee.player());
        if (target != null) {
            messenger.send(target, "player.invite-received", unparsed("claim", claim.id()), parsed("name", claim.displayName()),
                    unparsed("inviter", inviter.profile()), unparsed("profile", invitee.profile()), unparsed("minutes", minutes));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.ownIds(sender, args[0]);
            case 2 -> support.characters().onlinePlayers(args[1]);
            case 3 -> support.characters().profiles(args[1], args[2]);
            default -> List.of();
        };
    }
}
