package org.skysongdev.skysongClaims.limit;

import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Who a limit counts for: one character, or a whole player account when the type's limit is
 * per account.
 *
 * @param player    the player account
 * @param character the character, empty when the limit is per account
 */
public record LimitSubject(UUID player, Optional<CharacterId> character) {
    public LimitSubject {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(character, "character");
        character.ifPresent(id -> {
            if (!id.player().equals(player)) {
                throw new IllegalArgumentException("The character " + id + " is not of player " + player);
            }
        });
    }

    public static LimitSubject ofCharacter(CharacterId character) {
        return new LimitSubject(character.player(), Optional.of(character));
    }

    public static LimitSubject ofAccount(UUID player) {
        return new LimitSubject(player, Optional.empty());
    }

    /** @return whether the character's Claims count toward this subject */
    public boolean counts(CharacterId owner) {
        return character.map(owner::equals).orElseGet(() -> owner.player().equals(player));
    }
}
