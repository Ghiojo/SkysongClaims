package org.skysongdev.skysongClaims.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;

/**
 * Who owns which Claim. The intended buy flow: call {@link #canOwn}, take the money, then
 * {@link #addOwner} (or {@link #transfer}); if that doesn't answer {@link OwnershipCheck#OK},
 * refund. The API checks no permissions: the caller checks its player.
 *
 * <p>Main thread only. Methods taking a Claim id throw {@link IllegalArgumentException} when no
 * Claim has it.
 */
@ApiStatus.NonExtendable
public interface Ownership {
    @NotNull List<ClaimOwner> owners(@NotNull String claimId);

    boolean isOwner(@NotNull String claimId, @NotNull CharacterId character);

    /** @return whether the character may become an owner now, and if not, why */
    @NotNull OwnershipCheck canOwn(@NotNull CharacterId character, @NotNull String claimId);

    /**
     * Makes the character an owner, if {@link #canOwn} allows it. Fires {@code ClaimOwnerAddEvent}.
     *
     * @param plugin the plugin asking
     * @return {@link OwnershipCheck#OK} when it is now an owner, otherwise why it isn't
     */
    @NotNull OwnershipCheck addOwner(@NotNull Plugin plugin, @NotNull String claimId, @NotNull CharacterId character);

    /**
     * Removes an owner. When it was the last, the Claim is unowned again: a Free Claim reopens
     * every slot and its groups are cleared. Fires {@code ClaimOwnerRemoveEvent} with reason {@code API}.
     *
     * @return false when it wasn't an owner
     */
    boolean removeOwner(@NotNull Plugin plugin, @NotNull String claimId, @NotNull CharacterId character);

    /**
     * Hands one owner's place to another character in one step, for a sale or a gift between
     * players. The Claim keeps its groups. {@code to} must be allowed to own it, except that the
     * slot {@code from} frees counts as open.
     *
     * @return {@link OwnershipCheck#OK} when done; {@link OwnershipCheck#UNKNOWN_CHARACTER} also
     *         when {@code from} isn't an owner
     */
    @NotNull OwnershipCheck transfer(@NotNull Plugin plugin, @NotNull String claimId, @NotNull CharacterId from,
                                     @NotNull CharacterId to);

    /** Removes every owner, e.g. when a rent runs out. The Claim's groups are cleared. */
    void clearOwners(@NotNull Plugin plugin, @NotNull String claimId);
}
