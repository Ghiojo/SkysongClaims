package org.skysongdev.skysongClaims.sign;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongClaims.claim.Claim;

import java.util.Optional;

/** A button in a Claim sign's menu, such as Take slot, or SkysongEconomy's Buy. */
public interface MenuButton {
    /** @return the item to show this viewer for this Claim, or empty to hide the button */
    Optional<ItemStack> icon(Player viewer, Claim claim);

    /** Runs when the viewer clicks the button. The menu closes first. */
    void click(Player viewer, Claim claim);
}
