package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims list [type]}: every Claim, or those of one type, one line each. */
final class ListClaims extends LeafCommand {
    private final StaffSupport support;

    ListClaims(StaffSupport support) {
        super(support.messenger(), "list", "[type]", 0, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String type = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : null;
        List<Claim> claims = support.state().all().stream()
                .filter(claim -> type == null || claim.typeId().equals(type))
                .toList();
        if (claims.isEmpty()) {
            messenger.send(sender, "claims.list.empty");
            return;
        }
        messenger.send(sender, "claims.list.header", unparsed("count", Integer.toString(claims.size())));
        for (Claim claim : claims) {
            messenger.send(sender, claim.free() ? "claims.list.line-free" : "claims.list.line", unparsed("claim", claim.id()),
                    parsed("name", claim.displayName()), unparsed("type", claim.typeId()),
                    unparsed("owners", Integer.toString(claim.owners().size())),
                    unparsed("slots", Integer.toString(claim.slots().orElse(0))));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.arguments().typeIds(args[0]) : List.of();
    }
}
