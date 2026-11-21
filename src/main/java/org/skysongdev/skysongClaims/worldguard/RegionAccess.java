package org.skysongdev.skysongClaims.worldguard;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What WorldGuard must enforce on one region of a Claim: who owns it, who may build, and which
 * flags are open to everyone. It replaces whatever the region had in its owners and members.
 *
 * @param owners  the players whose current character owns the Claim
 * @param members the other players whose current character is in a group that may build
 * @param openUse the flags set to {@code allow}, such as {@code use} and {@code chest-access}
 * @param flags   the flags the owners set, by name, with the value as typed
 */
public record RegionAccess(Set<UUID> owners, Set<UUID> members, List<String> openUse, Map<String, String> flags) {
    public RegionAccess {
        owners = Set.copyOf(owners);
        members = Set.copyOf(members);
        openUse = List.copyOf(openUse);
        flags = Map.copyOf(flags);
    }

    /** @return no owners, no members, and no flags to open: what a region gets back when it leaves a Claim */
    public static RegionAccess released() {
        return new RegionAccess(Set.of(), Set.of(), List.of(), Map.of());
    }
}
