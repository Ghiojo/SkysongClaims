package org.skysongdev.skysongClaims.commands.staff;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.OwnerCause;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaims addowner <id> <player> [profile] [-force]}: makes a character an owner.
 * Without a profile it is the one the player plays now; {@code -force} skips the type limit.
 */
final class AddOwner extends LeafCommand {
    static final String FORCE = "-force";
    private final StaffSupport support;
    private final OwnershipService ownership;

    AddOwner(StaffSupport support, OwnershipService ownership) {
        super(support.messenger(), "addowner", "<id> <player> [profile] [-force]", 2, null);
        this.support = support;
        this.ownership = ownership;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        List<String> words = new ArrayList<>(Arrays.asList(args));
        boolean force = words.removeIf(FORCE::equalsIgnoreCase);
        CharacterId character = support.characterArguments().character(words.get(1), words.size() > 2 ? words.get(2) : null);
        ownership.addOwner(words.get(0), character, OwnerCause.STAFF, ChangeCause.COMMAND, force);
        messenger.send(sender, "claims.owner-added", unparsed("claim", words.get(0)),
                unparsed("character", ClaimArguments.describe(character)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.arguments().claimIds(args[0]);
            case 2 -> support.characterArguments().onlinePlayers(args[1]);
            case 3 -> {
                List<String> options = new ArrayList<>(support.characterArguments().profiles(args[1], args[2]));
                options.addAll(ClaimArguments.startingWith(java.util.stream.Stream.of(FORCE), args[2]));
                yield options;
            }
            case 4 -> ClaimArguments.startingWith(java.util.stream.Stream.of(FORCE), args[3]);
            default -> List.of();
        };
    }
}
