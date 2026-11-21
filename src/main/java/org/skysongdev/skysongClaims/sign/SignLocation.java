package org.skysongdev.skysongClaims.sign;

import org.bukkit.block.Block;

import java.util.Objects;

/** Where a Claim sign stands: a block in a world. */
public record SignLocation(String world, int x, int y, int z) {
    public SignLocation {
        Objects.requireNonNull(world, "world");
    }

    public static SignLocation of(Block block) {
        return new SignLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /** @return whether it is in the 16x16 chunk at those chunk coordinates */
    public boolean inChunk(String chunkWorld, int chunkX, int chunkZ) {
        return world.equals(chunkWorld) && (x >> 4) == chunkX && (z >> 4) == chunkZ;
    }

    @Override
    public String toString() {
        return world + " " + x + " " + y + " " + z;
    }
}
