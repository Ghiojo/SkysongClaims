package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * A Claim type from {@code types.yml}.
 *
 * @param id          its key in {@code types.yml}
 * @param displayName the name shown to players, in MiniMessage
 * @param limit       how many Claims of it one subject may own before {@link LimitRule}s
 * @param limitScope  whether the limit counts per character or per account
 * @param maxOwners   how many owners a new Claim of it has room for
 * @param freeClaim   whether new Claims of it are Free Claims
 * @param extra       its {@code extra:} section, as plain unmodifiable maps, lists and values,
 *                    for settings other plugins read (e.g. SkysongEconomy price modifiers)
 */
public record ClaimTypeInfo(
        @NotNull String id,
        @NotNull String displayName,
        int limit,
        @NotNull LimitScope limitScope,
        int maxOwners,
        boolean freeClaim,
        @NotNull Map<String, Object> extra) {

    public ClaimTypeInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(limitScope, "limitScope");
        extra = Map.copyOf(extra);
    }
}
