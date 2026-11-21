package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/** Reads the Claim types of {@code types.yml}, as of the last reload. Main thread only. */
@ApiStatus.NonExtendable
public interface ClaimTypes {
    /** @return every type, in file order */
    @NotNull List<ClaimTypeInfo> all();

    @NotNull Optional<ClaimTypeInfo> find(@NotNull String typeId);
}
