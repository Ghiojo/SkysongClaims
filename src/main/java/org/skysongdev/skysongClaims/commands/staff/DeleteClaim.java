package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaims delete <id> [confirm]}: deletes a Claim with its owners and groups. The
 * WorldGuard regions stay. Without {@code confirm} it only says what will happen.
 */
final class DeleteClaim extends LeafCommand {
    private static final String CONFIRM = "confirm";
    private final StaffSupport support;

    DeleteClaim(StaffSupport support) {
        super(support.messenger(), "delete", "<id> [confirm]", 1, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Claim claim = support.claims().require(args[0]);
        if (args.length < 2 || !args[1].equalsIgnoreCase(CONFIRM)) {
            messenger.send(sender, "claims.delete-confirm", unparsed("claim", claim.id()),
                    unparsed("owners", Integer.toString(claim.owners().size())));
            return;
        }
        support.claims().delete(claim.id(), ChangeCause.COMMAND);
        messenger.send(sender, "claims.deleted", unparsed("claim", claim.id()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> List.of(CONFIRM);
            default -> List.of();
        };
    }
}
