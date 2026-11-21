package org.skysongdev.skysongClaims.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A button another plugin adds to the menu a Claim sign opens, such as SkysongEconomy's Buy.
 * Add it with {@link ClaimSigns#addMenuButton}. Buttons show in the menu's bottom row, in the
 * order they were added.
 */
public interface MenuButton {
    /**
     * @param viewer the player opening the menu
     * @param claim  the Claim the sign shows
     * @return the item to show, or empty to hide the button for this viewer and Claim
     */
    @NotNull Optional<ItemStack> icon(@NotNull Player viewer, @NotNull ClaimInfo claim);

    /** Runs on the main thread when the viewer clicks the button. The menu is already closed. */
    void click(@NotNull Player viewer, @NotNull ClaimInfo claim);
}
