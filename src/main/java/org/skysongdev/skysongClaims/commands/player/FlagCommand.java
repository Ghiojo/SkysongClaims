package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.commands.staff.StaffSupport;
import org.skysongdev.skysongClaims.flag.FlagService;
import org.skysongdev.skysongClaims.group.Manager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaim flag <id> <flag> <value...|clear>}: an owner sets a WorldGuard flag on their whole Claim. */
final class FlagCommand extends LeafCommand {
    private static final String CLEAR = "clear";

    private final PlayerSupport support;
    private final FlagService flags;

    FlagCommand(PlayerSupport support, FlagService flags) {
        super(support.messenger(), "flag", "<id> <flag> <value...|clear>", 3, null);
        this.support = support;
        this.flags = flags;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Manager manager = sender.hasPermission(StaffSupport.ADMIN_PERMISSION)
                ? Manager.staff()
                : Manager.owner(support.characters().own(sender));
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (args.length == 3 && args[2].equalsIgnoreCase(CLEAR)) {
            flags.clear(manager, args[0], args[1]);
            messenger.send(sender, "flag.cleared", unparsed("flag", args[1]), unparsed("claim", args[0]));
            return;
        }
        flags.set(manager, args[0], args[1], value);
        messenger.send(sender, "flag.set", unparsed("flag", args[1]), unparsed("claim", args[0]), unparsed("value", value));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> sender.hasPermission(StaffSupport.ADMIN_PERMISSION)
                    ? ClaimArguments.startingWith(support.state().all().stream().map(Claim::id), args[0])
                    : support.ownIds(sender, args[0]);
            case 2 -> support.state().find(args[0])
                    .map(claim -> ClaimArguments.startingWith(flags.editable(claim).stream(), args[1]))
                    .orElse(List.of());
            case 3 -> ClaimArguments.startingWith(Stream.of(CLEAR), args[2]);
            default -> List.of();
        };
    }
}
