package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimLocator;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.commands.CharacterArguments;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.ownership.InviteService;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** What every {@code /sclaim} command uses. */
public record PlayerSupport(
        ClaimState state,
        ClaimLocator locator,
        OwnershipService ownership,
        InviteService invites,
        Limits limits,
        Supplier<ClaimTypeRegistry> types,
        CharacterArguments characters,
        Messenger messenger) {

    /** Lets players use {@code /sclaim}. */
    public static final String USE_PERMISSION = "skysongclaims.use";

    /** @throws RefusedException {@code player.not-in-claim} when the player stands in no Claim */
    public Claim claimHere(CommandSender sender) {
        Player player = CharacterArguments.player(sender);
        return locator.at(player.getLocation()).orElseThrow(() -> RefusedException.of("player.not-in-claim"));
    }

    /** @return the Claim named by {@code id}, or the one the sender stands in */
    public Claim claimOrHere(CommandSender sender, Optional<String> id) {
        return id.map(claimId -> state.find(claimId).orElseThrow(() -> RefusedException.of("claim.unknown", "claim", claimId)))
                .orElseGet(() -> claimHere(sender));
    }

    /** Completes the ids of the Claims the sender's character owns. */
    public List<String> ownIds(CommandSender sender, String typed) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        try {
            CharacterId character = characters.own(sender);
            return ClaimArguments.startingWith(state.ownedBy(character).stream().map(Claim::id), typed);
        } catch (RefusedException noCharacter) {
            return List.of();
        }
    }
}
