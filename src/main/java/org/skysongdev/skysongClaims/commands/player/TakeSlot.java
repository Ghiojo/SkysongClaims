package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongstats.api.CharacterId;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaim take}: takes a slot of the Free Claim you stand in, for the character you play. */
final class TakeSlot extends LeafCommand {
    private final PlayerSupport support;

    TakeSlot(PlayerSupport support) {
        super(support.messenger(), "take", "", 0, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        CharacterId character = support.characters().own(sender);
        Claim claim = support.ownership().take(character, support.claimHere(sender).id());
        messenger.send(sender, "player.taken", unparsed("claim", claim.id()), parsed("name", claim.displayName()),
                unparsed("open", Integer.toString(claim.openSlots().orElseThrow())));
    }
}
