package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongClaims.commands.CharacterArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.SignLocation;
import org.skysongdev.skysongClaims.sign.SignService;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** {@code /sclaims sign <id>} and {@code /sclaims unsign}: link or unlink the sign you look at. */
final class SignCommands {
    private static final int REACH = 6;

    private SignCommands() {
    }

    /** @throws RefusedException {@code signs.look-at-sign} when the player looks at no block */
    static SignLocation lookedAt(CommandSender sender) {
        Player player = CharacterArguments.player(sender);
        Block block = player.getTargetBlockExact(REACH);
        if (block == null) {
            throw RefusedException.of("signs.look-at-sign");
        }
        return SignLocation.of(block);
    }

    static final class Link extends LeafCommand {
        private final StaffSupport support;
        private final SignService signs;

        Link(StaffSupport support, SignService signs) {
            super(support.messenger(), "sign", "<id>", 1, null);
            this.support = support;
            this.signs = signs;
        }

        @Override
        protected void run(CommandSender sender, String[] args) {
            ClaimSign sign = signs.link(args[0], lookedAt(sender));
            messenger.send(sender, "signs.linked", unparsed("claim", sign.claimId()));
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return args.length == 1 ? support.arguments().claimIds(args[0]) : List.of();
        }
    }

    static final class Unlink extends LeafCommand {
        private final SignService signs;

        Unlink(StaffSupport support, SignService signs) {
            super(support.messenger(), "unsign", "", 0, null);
            this.signs = signs;
        }

        @Override
        protected void run(CommandSender sender, String[] args) {
            ClaimSign sign = signs.unlink(lookedAt(sender));
            messenger.send(sender, "signs.unlinked-from", unparsed("claim", sign.claimId()));
        }
    }
}
