package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims rename <id> <name...>}: the name players see, in MiniMessage. */
final class RenameClaim extends LeafCommand {
    private final StaffSupport support;

    RenameClaim(StaffSupport support) {
        super(support.messenger(), "rename", "<id> <name...>", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Claim claim = support.claims().rename(args[0], name, ChangeCause.COMMAND);
        messenger.send(sender, "claims.renamed", unparsed("claim", claim.id()), parsed("name", claim.displayName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.arguments().claimIds(args[0]) : List.of();
    }
}
