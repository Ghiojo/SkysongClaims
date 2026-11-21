package org.skysongdev.skysongClaims.support;

import org.skysongdev.skysongClaims.character.CharacterDirectory;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** SkysongStats characters for tests. */
public final class FakeCharacters implements CharacterDirectory {
    private final Set<CharacterId> existing = new LinkedHashSet<>();
    private final Map<UUID, CharacterId> active = new HashMap<>();

    /** Adds a character and makes it the one its player plays. */
    public CharacterId play(UUID player, String profile) {
        CharacterId character = add(player, profile);
        active.put(player, character);
        return character;
    }

    /** Adds a character the player isn't playing. */
    public CharacterId add(UUID player, String profile) {
        CharacterId character = new CharacterId(player, profile);
        existing.add(character);
        return character;
    }

    public void delete(CharacterId character) {
        existing.remove(character);
        active.remove(character.player(), character);
    }

    @Override
    public Optional<CharacterId> active(UUID player) {
        return Optional.ofNullable(active.get(player));
    }

    @Override
    public List<CharacterId> all(UUID player) {
        List<CharacterId> all = new ArrayList<>();
        existing.stream().filter(character -> character.player().equals(player)).forEach(all::add);
        return all;
    }

    @Override
    public boolean exists(CharacterId character) {
        return existing.contains(character);
    }
}
