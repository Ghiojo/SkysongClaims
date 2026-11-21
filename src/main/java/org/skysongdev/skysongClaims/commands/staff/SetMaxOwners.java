package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims maxowners <id> <number>}: how many owners a Claim has room for. */
final class SetMaxOwners extends LeafCommand {
    private final StaffSupport support;

    SetMaxOwners(StaffSupport support) {
        super(support.messenger(), "maxowners", "<id> <number>", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        int slots = Numbers.parse(args[1]);
        support.claims().setMaxOwners(args[0], slots, ChangeCause.COMMAND);
        messenger.send(sender, "claims.max-owners-set", unparsed("claim", args[0]), unparsed("slots", Integer.toString(slots)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.arguments().claimIds(args[0]) : List.of();
    }
}
