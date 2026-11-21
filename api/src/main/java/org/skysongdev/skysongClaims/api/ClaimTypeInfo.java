package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A Claim type from {@code types.yml}.
 *
 * @param id          its key in {@code types.yml}
 * @param displayName the name shown to players, in MiniMessage
 * @param limit       how many Claims of it one subject may own before {@link LimitRule}s
 * @param limitScope  whether the limit counts per character or per account
 * @param freeClaimSlots the slots of its new Claims, which are Free Claims; empty when its Claims
 *                       are bought, with no cap on owners
 * @param extra       its {@code extra:} section, as plain unmodifiable maps, lists and values,
 *                    for settings other plugins read (e.g. SkysongEconomy price modifiers)
 */
public record ClaimTypeInfo(
        @NotNull String id,
        @NotNull String displayName,
        int limit,
        @NotNull LimitScope limitScope,
        @NotNull OptionalInt freeClaimSlots,
        @NotNull Map<String, Object> extra) {

    public ClaimTypeInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(limitScope, "limitScope");
        Objects.requireNonNull(freeClaimSlots, "freeClaimSlots");
        extra = Map.copyOf(extra);
    }

    /** @return whether new Claims of it are Free Claims */
    public boolean freeClaim() {
        return freeClaimSlots.isPresent();
    }
}
