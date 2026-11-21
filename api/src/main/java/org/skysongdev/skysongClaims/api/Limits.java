package org.skysongdev.skysongClaims.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

/**
 * How many Claims of each type someone may own: the type's base limit plus every registered
 * {@link LimitRule}. Someone over their limit keeps what they own but can't take more. Main
 * thread only; unknown type ids throw {@link IllegalArgumentException}.
 */
@ApiStatus.NonExtendable
public interface Limits {
    /** @return who the type's limit counts for when this character owns one: itself, or its whole account */
    @NotNull LimitSubject subject(@NotNull CharacterId character, @NotNull String typeId);

    /** @return the base limit plus every rule's bonus, never below 0 */
    int limit(@NotNull LimitSubject subject, @NotNull String typeId);

    /** @return how many Claims of the type the subject owns */
    int count(@NotNull LimitSubject subject, @NotNull String typeId);

    /** @return how many more the subject may own, never below 0 */
    int remaining(@NotNull LimitSubject subject, @NotNull String typeId);

    /**
     * Adds a live rule, or replaces this plugin's rule with that id. Rules are removed when
     * their plugin disables.
     */
    void registerRule(@NotNull Plugin plugin, @NotNull String id, @NotNull LimitRule rule);

    void unregisterRule(@NotNull Plugin plugin, @NotNull String id);
}
