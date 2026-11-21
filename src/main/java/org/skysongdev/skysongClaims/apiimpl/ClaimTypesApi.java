package org.skysongdev.skysongClaims.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimTypeInfo;
import org.skysongdev.skysongClaims.api.ClaimTypes;
import org.skysongdev.skysongClaims.type.ClaimType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link ClaimTypes} over the types loaded from types.yml. */
final class ClaimTypesApi implements ClaimTypes {
    private final ApiLookup lookup;

    ClaimTypesApi(ApiLookup lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    @Override
    public @NotNull List<ClaimTypeInfo> all() {
        lookup.checkThread();
        return lookup.types().all().stream().map(ClaimType::info).toList();
    }

    @Override
    public @NotNull Optional<ClaimTypeInfo> find(@NotNull String typeId) {
        lookup.checkThread();
        return lookup.types().find(typeId).map(ClaimType::info);
    }
}
