package org.skysongdev.skysongClaims.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongClaims.character.CharacterDirectory;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Turns {@code <player> [profile]} arguments into characters, and completes them. */
public final class CharacterArguments {
    private final CharacterDirectory characters;

    public CharacterArguments(CharacterDirectory characters) {
        this.characters = characters;
    }

    /** @throws RefusedException {@code command.players-only} when the sender isn't a player */
    public static Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw RefusedException.of("command.players-only");
    }

    /** @throws RefusedException when the sender isn't a player or plays no character */
    public CharacterId own(CommandSender sender) {
        Player player = player(sender);
        return characters.active(player.getUniqueId())
                .orElseThrow(() -> RefusedException.of("command.you-have-no-character"));
    }

    /**
     * Finds a player's character: the named profile, or the one they play now.
     *
     * @param profile a profile name, matched ignoring case, or {@code null} for the active one
     * @throws RefusedException when the player or the character is unknown
     */
    public CharacterId character(String playerName, @Nullable String profile) {
        OfflinePlayer player = findPlayer(playerName)
                .orElseThrow(() -> RefusedException.of("command.unknown-player", "player", playerName));
        if (profile == null) {
            return characters.active(player.getUniqueId())
                    .orElseThrow(() -> RefusedException.of("command.no-active-character", "player", playerName));
        }
        return characters.all(player.getUniqueId()).stream()
                .filter(character -> character.profile().equalsIgnoreCase(profile))
                .findFirst()
                .orElseThrow(() -> RefusedException.of("command.unknown-character", "player", playerName, "profile", profile));
    }

    public List<String> onlinePlayers(String typed) {
        return ClaimArguments.startingWith(Bukkit.getOnlinePlayers().stream().map(Player::getName), typed);
    }

    public List<String> profiles(String playerName, String typed) {
        Stream<String> profiles = findPlayer(playerName).stream()
                .flatMap(player -> characters.all(player.getUniqueId()).stream())
                .map(CharacterId::profile);
        return ClaimArguments.startingWith(profiles, typed);
    }

    /** @return the online player with that name, or a known offline one */
    public static Optional<OfflinePlayer> findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        return Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(name));
    }
}
