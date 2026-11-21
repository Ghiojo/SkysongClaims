package org.skysongdev.skysongClaims.sign;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

import java.util.List;
import java.util.Optional;

/** {@link SignBlocks} on Bukkit's sign block states. */
public final class BukkitSignBlocks implements SignBlocks {

    @Override
    public boolean isSign(SignLocation location) {
        return sign(location).isPresent();
    }

    @Override
    public Result write(SignLocation location, List<Component> lines) {
        World world = Bukkit.getWorld(location.world());
        if (world == null || !world.isChunkLoaded(location.x() >> 4, location.z() >> 4)) {
            return Result.NOT_LOADED;
        }
        Optional<Sign> found = sign(location);
        if (found.isEmpty()) {
            return Result.NOT_A_SIGN;
        }
        Sign sign = found.get();
        SignSide front = sign.getSide(Side.FRONT);
        for (int line = 0; line < lines.size() && line < 4; line++) {
            front.line(line, lines.get(line));
        }
        sign.setWaxed(true);
        sign.update(true, false);
        return Result.WRITTEN;
    }

    @Override
    public void release(SignLocation location) {
        sign(location).ifPresent(sign -> {
            sign.setWaxed(false);
            sign.update(true, false);
        });
    }

    private static Optional<Sign> sign(SignLocation location) {
        World world = Bukkit.getWorld(location.world());
        if (world == null || !world.isChunkLoaded(location.x() >> 4, location.z() >> 4)) {
            return Optional.empty();
        }
        Block block = world.getBlockAt(location.x(), location.y(), location.z());
        return block.getState() instanceof Sign sign ? Optional.of(sign) : Optional.empty();
    }
}
