package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongstats.api.CharacterId;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A group of characters that an owner gave rights in their Claim, such as "Family" with
 * {@link GroupPermission#BUILD}.
 *
 * @param id          the name owners type, lower case
 * @param displayName the name shown in lists
 * @param permissions what the members may do
 * @param members     the characters in the group, in the order they were added
 */
public record ClaimGroup(String id, String displayName, Set<GroupPermission> permissions, Set<CharacterId> members) {
    public ClaimGroup {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        permissions = Set.copyOf(permissions);
        members = java.util.Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    /** @return the group without that member */
    public ClaimGroup withoutMember(CharacterId member) {
        Set<CharacterId> remaining = new LinkedHashSet<>(members);
        remaining.remove(member);
        return new ClaimGroup(id, displayName, permissions, remaining);
    }

    public boolean has(GroupPermission permission) {
        return permissions.contains(permission);
    }
}
