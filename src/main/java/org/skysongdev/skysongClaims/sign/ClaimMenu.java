package org.skysongdev.skysongClaims.sign;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** One open Claim sign menu: its inventory and what each slot does when clicked. */
public final class ClaimMenu implements InventoryHolder {
    /** The menu has three rows. */
    static final int SIZE = 27;

    private final String claimId;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
    private final Inventory inventory;

    ClaimMenu(String claimId, Component title) {
        this.claimId = claimId;
        this.inventory = Bukkit.createInventory(this, SIZE, title);
    }

    public String claimId() {
        return claimId;
    }

    void set(int slot, ItemStack item, Consumer<Player> action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    /** Runs what the slot does, when it does anything. */
    public void click(int slot, Player player) {
        Consumer<Player> action = actions.get(slot);
        if (action != null) {
            action.accept(player);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
