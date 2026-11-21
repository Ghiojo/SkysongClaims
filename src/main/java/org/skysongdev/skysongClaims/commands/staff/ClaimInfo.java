package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.type.ClaimType;

import java.util.List;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims info <id>}: everything about one Claim. */
final class ClaimInfo extends LeafCommand {
    private final StaffSupport support;

    ClaimInfo(StaffSupport support) {
        super(support.messenger(), "info", "<id>", 1, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Claim claim = support.claims().require(args[0]);
        String typeName = support.types().get().find(claim.typeId()).map(ClaimType::displayName)
                .orElse(claim.typeId());
        messenger.send(sender, "claims.info.header", unparsed("claim", claim.id()), parsed("name", claim.displayName()));
        messenger.send(sender, support.types().get().find(claim.typeId()).isPresent()
                        ? "claims.info.type" : "claims.info.type-unknown",
                unparsed("type", claim.typeId()), parsed("type_name", typeName));
        messenger.send(sender, claim.free() ? "claims.info.free" : "claims.info.bought",
                unparsed("owners", Integer.toString(claim.owners().size())),
                unparsed("slots", Integer.toString(claim.maxOwners())),
                unparsed("open", Integer.toString(claim.openSlots())));
        messenger.send(sender, "claims.info.regions", unparsed("regions",
                claim.regions().stream().map(ClaimRegion::toString).collect(Collectors.joining(", "))));
        messenger.send(sender, claim.isOwned() ? "claims.info.owners" : "claims.info.no-owners",
                unparsed("owners", claim.owners().stream().map(ClaimOwner::character).map(ClaimArguments::describe)
                        .collect(Collectors.joining(", "))));
        if (!claim.groups().isEmpty()) {
            messenger.send(sender, "claims.info.groups", unparsed("groups",
                    claim.groups().stream().map(group -> group.id() + " (" + group.members().size() + ")")
                            .collect(Collectors.joining(", "))));
        }
        if (!claim.flags().isEmpty()) {
            messenger.send(sender, "claims.info.flags", unparsed("flags", claim.flags().entrySet().stream()
                    .map(flag -> flag.getKey() + ": " + flag.getValue()).collect(Collectors.joining(", "))));
        }
        claim.saleSetBy().ifPresent(plugin -> messenger.send(sender, "claims.info.for-sale", unparsed("plugin", plugin)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.arguments().claimIds(args[0]) : List.of();
    }
}
