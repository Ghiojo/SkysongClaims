package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.type.ClaimType;

import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaim info}: the Claim you stand in. */
final class HereInfo extends LeafCommand {
    private final PlayerSupport support;

    HereInfo(PlayerSupport support) {
        super(support.messenger(), "info", "", 0, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Claim claim = support.claimHere(sender);
        String typeName = support.types().get().find(claim.typeId()).map(ClaimType::displayName).orElse(claim.typeId());
        messenger.send(sender, "player.info.header", parsed("name", claim.displayName()), parsed("type_name", typeName));
        messenger.send(sender, claim.isOwned() ? "player.info.owners" : "player.info.no-owners",
                unparsed("owners", claim.owners().stream().map(ClaimOwner::character).map(ClaimArguments::describe)
                        .collect(Collectors.joining(", "))));
        if (claim.free()) {
            messenger.send(sender, claim.openSlots() > 0 ? "player.info.free-open" : "player.info.free-full",
                    unparsed("open", Integer.toString(claim.openSlots())), unparsed("slots", Integer.toString(claim.maxOwners())));
        } else if (claim.isForSale()) {
            messenger.send(sender, "player.info.for-sale");
        }
    }
}
