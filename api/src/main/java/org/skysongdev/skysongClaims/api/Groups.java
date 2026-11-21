package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;

/**
 * Reads the build groups owners made. Read only: owners manage them with commands. Main thread
 * only; unknown Claim ids throw {@link IllegalArgumentException}.
 */
@ApiStatus.NonExtendable
public interface Groups {
    @NotNull List<ClaimGroup> of(@NotNull String claimId);

    /** @return whether the character owns the Claim or is in one of its groups with {@code build} */
    boolean canBuild(@NotNull CharacterId character, @NotNull String claimId);
}
