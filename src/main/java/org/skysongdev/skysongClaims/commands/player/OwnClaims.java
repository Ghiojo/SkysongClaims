package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.LimitScope;
import org.skysongdev.skysongClaims.api.LimitSubject;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaim list}: the Claims of the character you play, and how many of each type it may still own. */
final class OwnClaims extends LeafCommand {
    private final PlayerSupport support;

    OwnClaims(PlayerSupport support) {
        super(support.messenger(), "list", "", 0, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        CharacterId character = support.characters().own(sender);
        List<Claim> owned = support.state().ownedBy(character);
        messenger.send(sender, owned.isEmpty() ? "player.list.none" : "player.list.header",
                unparsed("count", Integer.toString(owned.size())));
        for (Claim claim : owned) {
            messenger.send(sender, "player.list.line", unparsed("claim", claim.id()), parsed("name", claim.displayName()));
        }
        for (ClaimType type : support.types().get().all()) {
            LimitSubject subject = Limits.subject(character, type);
            messenger.send(sender, type.limitScope() == LimitScope.ACCOUNT
                            ? "player.list.limit-account" : "player.list.limit",
                    parsed("type_name", type.displayName()),
                    unparsed("count", Integer.toString(support.limits().count(subject, type))),
                    unparsed("limit", Integer.toString(support.limits().limit(subject, type))));
        }
    }
}
