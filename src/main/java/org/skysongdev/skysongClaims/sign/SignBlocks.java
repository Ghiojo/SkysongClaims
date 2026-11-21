package org.skysongdev.skysongClaims.sign;

import net.kyori.adventure.text.Component;

import java.util.List;

/** The sign blocks in the world. Implemented on Bukkit, faked in tests. */
public interface SignBlocks {
    /** What happened when a sign was written. */
    enum Result {
        WRITTEN,
        /** Its chunk isn't loaded; it is written when the chunk loads. */
        NOT_LOADED,
        /** The block is no longer a sign. */
        NOT_A_SIGN
    }

    /** @return whether a sign stands there now; false when its chunk isn't loaded */
    boolean isSign(SignLocation location);

    /** Writes the four lines on the front and waxes the sign so players can't edit it. Never loads a chunk. */
    Result write(SignLocation location, List<Component> lines);

    /** Unwaxes the sign, so it can be edited again after it is unlinked. */
    void release(SignLocation location);
}
