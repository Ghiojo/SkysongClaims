package org.skysongdev.skysongClaims.worldguard;

import org.skysongdev.skysongClaims.character.CharacterDirectory;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimGroup;
import org.skysongdev.skysongClaims.claim.ClaimOwner;
import org.skysongdev.skysongClaims.claim.GroupPermission;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.type.ClaimTypesParser;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Turns a Claim into what WorldGuard enforces. WorldGuard knows players, not characters, so a
 * player counts only while the character they play now has the right: switching to a
 * character with no rights takes building away at once.
 */
public final class AccessProjection {
    private final CharacterDirectory characters;
    private final Supplier<ClaimTypeRegistry> types;

    public AccessProjection(CharacterDirectory characters, Supplier<ClaimTypeRegistry> types) {
        this.characters = Objects.requireNonNull(characters, "characters");
        this.types = Objects.requireNonNull(types, "types");
    }

    public RegionAccess of(Claim claim) {
        Set<UUID> owners = new LinkedHashSet<>();
        for (ClaimOwner owner : claim.owners()) {
            addIfPlayed(owner.character(), owners);
        }
        Set<UUID> members = new LinkedHashSet<>();
        for (ClaimGroup group : claim.groups()) {
            if (group.has(GroupPermission.BUILD)) {
                group.members().forEach(member -> addIfPlayed(member, members));
            }
        }
        members.removeAll(owners);
        return new RegionAccess(owners, members, openUse(claim), claim.flags());
    }

    /** A Claim whose type was removed from types.yml keeps the standard open-use flags, so chests never lock. */
    private List<String> openUse(Claim claim) {
        return types.get().find(claim.typeId()).map(ClaimType::openUse).orElse(ClaimTypesParser.DEFAULT_OPEN_USE);
    }

    private void addIfPlayed(CharacterId character, Set<UUID> players) {
        if (characters.active(character.player()).filter(character::equals).isPresent()) {
            players.add(character.player());
        }
    }
}
