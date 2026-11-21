package org.skysongdev.skysongClaims.type;

import org.skysongdev.skysongClaims.api.ClaimTypeInfo;
import org.skysongdev.skysongClaims.api.LimitScope;

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
 * @param freeClaimSlots   the slots of a new Claim of this type, which is a Free Claim that players
 *                         take themselves; empty when its Claims are bought, with no cap on owners
 * @param openUse          the WorldGuard flags set to {@code allow} on every region of its Claims
 * @param extra            settings for other plugins, as plain unmodifiable maps, lists and values
 */
public record ClaimType(
        String id,
        String displayName,
        int limit,
        LimitScope limitScope,
        OptionalInt freeClaimSlots,
        List<String> openUse,
        Map<String, Object> extra) {

    public ClaimType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(limitScope, "limitScope");
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        Objects.requireNonNull(freeClaimSlots, "freeClaimSlots");
        if (freeClaimSlots.isPresent() && freeClaimSlots.getAsInt() < 1) {
            throw new IllegalArgumentException("A Free Claim type needs at least one slot: " + freeClaimSlots.getAsInt());
        }
        openUse = List.copyOf(openUse);
        extra = Map.copyOf(extra);
    }

    /** @return what the API shows of it: everything but its open-use flags */
    public ClaimTypeInfo info() {
        return new ClaimTypeInfo(id, displayName, limit, limitScope, freeClaimSlots, extra);
    }

    /** @return whether a new Claim of this type is a Free Claim */
    public boolean freeClaim() {
        return freeClaimSlots.isPresent();
    }
}
