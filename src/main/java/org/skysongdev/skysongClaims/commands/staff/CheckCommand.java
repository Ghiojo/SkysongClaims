package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.ClaimCheck;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;

import java.util.function.Supplier;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaims check}: lists Claim regions WorldGuard no longer has, releases characters
 * that no longer exist, and writes every Claim to WorldGuard again.
 */
final class CheckCommand extends LeafCommand {
    private final Supplier<ClaimCheck.Report> check;

    /** @param check runs the check and the WorldGuard sync, and says what it found */
    CheckCommand(StaffSupport support, Supplier<ClaimCheck.Report> check) {
        super(support.messenger(), "check", "", 0, null);
        this.check = check;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        ClaimCheck.Report report = check.get();
        for (ClaimCheck.Missing missing : report.missingRegions()) {
            messenger.send(sender, "claims.check.missing-region", unparsed("claim", missing.claimId()),
                    unparsed("region", missing.region().toString()));
        }
        for (ClaimCheck.Released released : report.released()) {
            messenger.send(sender, "claims.check.released", unparsed("claim", released.claimId()),
                    unparsed("character", ClaimArguments.describe(released.character())));
        }
        messenger.send(sender, "claims.check.done",
                unparsed("missing", Integer.toString(report.missingRegions().size())),
                unparsed("released", Integer.toString(report.released().size())));
    }
}
