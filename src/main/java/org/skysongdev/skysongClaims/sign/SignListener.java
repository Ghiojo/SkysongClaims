package org.skysongdev.skysongClaims.sign;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.commands.staff.StaffSupport;
import org.skysongdev.skysongClaims.messaging.Messenger;

import java.util.List;
import java.util.Objects;

/**
 * Keeps Claim signs working: right-clicking one opens its menu, nobody but staff can break,
 * edit or wax one, explosions, fire and pistons leave them alone, and signs are redrawn when
 * their chunk loads.
 */
public final class SignListener implements Listener {
    private final SignService signs;
    private final SignMenus menus;
    private final ClaimState claims;
    private final Messenger messenger;

    public SignListener(SignService signs, SignMenus menus, ClaimState claims, Messenger messenger) {
        this.signs = Objects.requireNonNull(signs, "signs");
        this.menus = Objects.requireNonNull(menus, "menus");
        this.claims = Objects.requireNonNull(claims, "claims");
        this.messenger = Objects.requireNonNull(messenger, "messenger");
    }

    private boolean linked(Block block) {
        return signs.at(SignLocation.of(block)).isPresent();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        signs.at(SignLocation.of(block)).flatMap(sign -> claims.find(sign.claimId())).ifPresent(claim -> {
            // Also stops honeycomb, dyes and the sign editor.
            event.setCancelled(true);
            menus.open(event.getPlayer(), claim);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEdit(SignChangeEvent event) {
        if (linked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!linked(event.getBlock())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission(StaffSupport.ADMIN_PERMISSION)) {
            event.setCancelled(true);
            messenger.send(player, "signs.protected");
            return;
        }
        signs.unlink(SignLocation.of(event.getBlock()));
        messenger.send(player, "signs.unlinked");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::linked);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::linked);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (linked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        cancelIfAnyLinked(event.getBlocks(), () -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        cancelIfAnyLinked(event.getBlocks(), () -> event.setCancelled(true));
    }

    private void cancelIfAnyLinked(List<Block> blocks, Runnable cancel) {
        if (blocks.stream().anyMatch(this::linked)) {
            cancel.run();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        signs.redrawChunk(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof ClaimMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < ClaimMenu.SIZE && event.getWhoClicked() instanceof Player player) {
            menu.click(event.getRawSlot(), player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof ClaimMenu) {
            event.setCancelled(true);
        }
    }
}
