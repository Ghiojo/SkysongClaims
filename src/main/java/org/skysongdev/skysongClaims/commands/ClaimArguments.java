package org.skysongdev.skysongClaims.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Reads and completes the arguments the Claim commands share: Claim ids, types and regions. */
public final class ClaimArguments {
    private final ClaimState claims;
    private final Supplier<ClaimTypeRegistry> types;
    private final RegionGateway regions;

    public ClaimArguments(ClaimState claims, Supplier<ClaimTypeRegistry> types, RegionGateway regions) {
        this.claims = claims;
        this.types = types;
        this.regions = regions;
    }

    /**
     * Reads {@code [world:]region}. Without a world it is the sender's own world.
     *
     * @throws RefusedException {@code command.region-needs-world} when the console leaves out the world
     */
    public ClaimRegion region(CommandSender sender, String typed) {
        int colon = typed.indexOf(':');
        if (colon > 0 && colon < typed.length() - 1) {
            return new ClaimRegion(typed.substring(0, colon), typed.substring(colon + 1));
        }
        if (sender instanceof Player player) {
            return new ClaimRegion(player.getWorld().getName(), typed);
        }
        throw RefusedException.of("command.region-needs-world", "region", typed);
    }

    public List<String> claimIds(String typed) {
        return startingWith(claims.all().stream().map(Claim::id), typed);
    }

    public List<String> typeIds(String typed) {
        return startingWith(types.get().all().stream().map(ClaimType::id), typed);
    }

    /** Completes regions of the sender's world, and {@code world:region} for the others. */
    public List<String> regionIds(CommandSender sender, String typed) {
        int colon = typed.indexOf(':');
        if (colon > 0) {
            String world = typed.substring(0, colon);
            return startingWith(regions.regionIds(world).stream().map(id -> world + ":" + id), typed);
        }
        Stream<String> own = sender instanceof Player player
                ? regions.regionIds(player.getWorld().getName()).stream()
                : Stream.empty();
        Stream<String> worlds = Bukkit.getWorlds().stream().map(world -> world.getName() + ":");
        return startingWith(Stream.concat(own, worlds), typed);
    }

    /** @return a character's name for messages: the profile, and the player when the names differ */
    public static String describe(CharacterId character) {
        String playerName = Bukkit.getOfflinePlayer(character.player()).getName();
        if (playerName == null || playerName.equalsIgnoreCase(character.profile())) {
            return character.profile();
        }
        return character.profile() + " (" + playerName + ")";
    }

    public static List<String> startingWith(Stream<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
