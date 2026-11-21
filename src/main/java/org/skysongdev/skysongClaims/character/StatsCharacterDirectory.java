package org.skysongdev.skysongClaims.character;

import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.SkysongStatsApi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** {@link CharacterDirectory} on the SkysongStats API. */
public final class StatsCharacterDirectory implements CharacterDirectory {
    private final SkysongStatsApi stats;

    public StatsCharacterDirectory(SkysongStatsApi stats) {
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    @Override
    public Optional<CharacterId> active(UUID player) {
        return stats.profiles().active(player);
    }

    @Override
    public List<CharacterId> all(UUID player) {
        return stats.profiles().all(player);
    }

    @Override
    public boolean exists(CharacterId character) {
        // Characters made by other plugins (NPCs) are known through characters(), not profiles().
        return stats.profiles().exists(character) || stats.characters().owner(character).isPresent();
    }
}
