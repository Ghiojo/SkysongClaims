package org.skysongdev.skysongClaims.type;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A kind of Claim from {@code types.yml}, such as a hovel or a manor. Its limit caps how many
 * Claims of the type one character (or one account) may own; other plugins can read its
 * {@code extra} section, for example SkysongEconomy for price modifiers.
 *
 * @param id               the key in {@code types.yml}, lower case
 * @param displayName      the name shown to players, in MiniMessage
 * @param limit            how many Claims of this type one subject may own before bonuses
 * @param limitScope       whether {@link #limit} counts per character or per account
 * @param maxOwners        how many owners a new Claim of this type has room for
 * @param freeClaim        whether a new Claim of this type starts as a Free Claim that players take themselves
 * @param openUse          the WorldGuard flags set to {@code allow} on every region of its Claims
 * @param extra            settings for other plugins, as plain unmodifiable maps, lists and values
 */
public record ClaimType(
        String id,
        String displayName,
        int limit,
        LimitScope limitScope,
        int maxOwners,
        boolean freeClaim,
        List<String> openUse,
        Map<String, Object> extra) {

    public ClaimType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(limitScope, "limitScope");
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        if (maxOwners < 1) {
            throw new IllegalArgumentException("maxOwners must be at least 1: " + maxOwners);
        }
        openUse = List.copyOf(openUse);
        extra = Map.copyOf(extra);
    }

    /** @return the number of owners a new Free Claim of this type has room for, empty when it isn't a Free Claim type */
    public OptionalInt freeClaimSlots() {
        return freeClaim ? OptionalInt.of(maxOwners) : OptionalInt.empty();
    }
}
