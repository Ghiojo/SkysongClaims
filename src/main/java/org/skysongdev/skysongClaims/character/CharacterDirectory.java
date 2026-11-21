package org.skysongdev.skysongClaims.character;

import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What SkysongClaims needs to know about SkysongStats characters. Works for offline players too. */
public interface CharacterDirectory {
    /** @return the character the player is playing now, empty when they have none */
    Optional<CharacterId> active(UUID player);

    /** @return every character of the player */
    List<CharacterId> all(UUID player);

    /** @return whether the character still exists: a player's profile, or a character another plugin made (an NPC) */
    boolean exists(CharacterId character);
}
