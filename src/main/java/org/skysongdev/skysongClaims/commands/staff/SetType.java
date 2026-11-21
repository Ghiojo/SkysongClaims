package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims settype <id> <type>}: changes a Claim's type. Owners keep it even over the new limit. */
final class SetType extends LeafCommand {
    private final StaffSupport support;

    SetType(StaffSupport support) {
        super(support.messenger(), "settype", "<id> <type>", 2, null);
        this.support = support;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        String type = args[1].toLowerCase(Locale.ROOT);
        support.claims().setType(args[0], type, ChangeCause.COMMAND);
        messenger.send(sender, "claims.type-set", unparsed("claim", args[0]), unparsed("type", type));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> support.arguments().typeIds(args[1]);
            default -> List.of();
        };
    }
}
