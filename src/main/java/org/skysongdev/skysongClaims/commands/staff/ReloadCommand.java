package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.config.ConfigException;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaims reload}: reads {@code config.yml}, {@code types.yml} and {@code messages.yml}
 * again. A broken file keeps the old config. Claims whose type is gone are listed.
 */
final class ReloadCommand extends LeafCommand {
    private final StaffSupport support;
    private final Runnable reload;

    /** @param reload reloads every file; throws {@link ConfigException} when one is invalid */
    ReloadCommand(StaffSupport support, Runnable reload) {
        super(support.messenger(), "reload", "", 0, null);
        this.support = support;
        this.reload = reload;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        try {
            reload.run();
        } catch (ConfigException e) {
            messenger.send(sender, "claims.reload-failed", unparsed("error", e.getMessage()));
            return;
        }
        messenger.send(sender, "claims.reloaded", unparsed("types", Integer.toString(support.types().get().all().size())));
        for (Claim claim : support.claims().withUnknownType()) {
            messenger.send(sender, "claims.unknown-type-warning", unparsed("claim", claim.id()), unparsed("type", claim.typeId()));
        }
    }
}
