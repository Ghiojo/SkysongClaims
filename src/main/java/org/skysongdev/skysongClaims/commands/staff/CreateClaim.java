package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims create <id> <type> <[world:]region...>}: makes an unowned Claim from WorldGuard regions. */
final class CreateClaim extends LeafCommand {
    private final StaffSupport support;

    CreateClaim(StaffSupport support) {
        super(support.messenger(), "create", "<id> <type> <[world:]region...>", 3, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        List<ClaimRegion> regions = Arrays.stream(args, 2, args.length)
                .map(typed -> support.arguments().region(sender, typed))
                .toList();
        Claim claim = support.claims().create(args[0].toLowerCase(Locale.ROOT), args[1].toLowerCase(Locale.ROOT),
                regions, ChangeCause.COMMAND);
        messenger.send(sender, claim.free() ? "claims.created-free" : "claims.created",
                unparsed("claim", claim.id()), unparsed("type", claim.typeId()),
                unparsed("regions", Integer.toString(claim.regions().size())),
                unparsed("slots", Integer.toString(claim.maxOwners())));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> List.of();
            case 2 -> support.arguments().typeIds(args[1]);
            default -> support.arguments().regionIds(sender, args[args.length - 1]);
        };
    }
}
