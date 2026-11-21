package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims addregion <id> <[world:]region>}: adds a WorldGuard region, from any world, to a Claim. */
final class AddRegion extends LeafCommand {
    private final StaffSupport support;

    AddRegion(StaffSupport support) {
        super(support.messenger(), "addregion", "<id> <[world:]region>", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        ClaimRegion region = support.arguments().region(sender, args[1]);
        support.claims().addRegion(args[0], region, ChangeCause.COMMAND);
        messenger.send(sender, "claims.region-added", unparsed("claim", args[0]), unparsed("region", region.toString()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> support.arguments().regionIds(sender, args[1]);
            default -> List.of();
        };
    }
}
