package org.skysongdev.skysongClaims.api;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The signs staff linked to Claims. SkysongClaims writes their first three lines; the fourth is
 * for another plugin's price, saved by SkysongClaims, so it is set once and kept across
 * restarts. Main thread only; unknown Claim ids throw {@link IllegalArgumentException}.
 */
@ApiStatus.NonExtendable
public interface ClaimSigns {
    @NotNull List<SignInfo> of(@NotNull String claimId);

    /** Sets the fourth line of every sign of the Claim. A sign linked later starts blank: listen to {@code ClaimSignChangeEvent}. */
    void setPriceLine(@NotNull Plugin plugin, @NotNull String claimId, @NotNull Component line);

    void clearPriceLine(@NotNull Plugin plugin, @NotNull String claimId);

    /** Adds a button to every Claim sign menu, or replaces this plugin's button with that id. Removed when the plugin disables. */
    void addMenuButton(@NotNull Plugin plugin, @NotNull String id, @NotNull MenuButton button);

    void removeMenuButton(@NotNull Plugin plugin, @NotNull String id);
}
