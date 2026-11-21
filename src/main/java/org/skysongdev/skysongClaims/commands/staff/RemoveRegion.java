package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims removeregion <id> <[world:]region>}: takes a region out of a Claim. The WorldGuard region stays. */
final class RemoveRegion extends LeafCommand {
    private final StaffSupport support;

    RemoveRegion(StaffSupport support) {
        super(support.messenger(), "removeregion", "<id> <[world:]region>", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        ClaimRegion region = support.arguments().region(sender, args[1]);
        support.claims().removeRegion(args[0], region, ChangeCause.COMMAND);
        messenger.send(sender, "claims.region-removed", unparsed("claim", args[0]), unparsed("region", region.toString()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> support.state().find(args[0])
                    .map(claim -> ClaimArguments.startingWith(claim.regions().stream().map(ClaimRegion::toString), args[1]))
                    .orElse(List.of());
            default -> List.of();
        };
    }
}
