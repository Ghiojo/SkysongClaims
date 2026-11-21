package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.messaging.RefusedException;

import java.util.List;
import java.util.OptionalInt;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaims free <id> <true|false> [max-owners]}: makes a Claim a Free Claim that players
 * take themselves, or not, and optionally sets its owner slots.
 */
final class SetFree extends LeafCommand {
    private final StaffSupport support;

    SetFree(StaffSupport support) {
        super(support.messenger(), "free", "<id> <true|false> [max-owners]", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        boolean free = switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw RefusedException.of("command.not-true-or-false", "input", args[1]);
        };
        OptionalInt slots = args.length > 2 ? OptionalInt.of(Numbers.parse(args[2])) : OptionalInt.empty();
        Claim claim = support.claims().setFree(args[0], free, slots, ChangeCause.COMMAND);
        messenger.send(sender, free ? "claims.free-on" : "claims.free-off", unparsed("claim", claim.id()),
                unparsed("slots", Integer.toString(claim.maxOwners())));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> List.of("true", "false");
            default -> List.of();
        };
    }
}
