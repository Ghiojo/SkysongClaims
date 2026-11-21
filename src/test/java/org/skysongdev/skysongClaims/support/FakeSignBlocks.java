package org.skysongdev.skysongClaims.support;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.skysongdev.skysongClaims.sign.SignBlocks;
import org.skysongdev.skysongClaims.sign.SignLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sign blocks for tests: only the places added hold a sign; their last lines are kept as plain text. */
public final class FakeSignBlocks implements SignBlocks {
    private final Set<SignLocation> signs = new HashSet<>();
    private final Set<SignLocation> unloaded = new HashSet<>();
    private final Map<SignLocation, List<String>> lines = new HashMap<>();
    private final Set<SignLocation> released = new HashSet<>();

    public FakeSignBlocks sign(SignLocation location) {
        signs.add(location);
        return this;
    }

    public void breakSign(SignLocation location) {
        signs.remove(location);
    }

    public void unload(SignLocation location) {
        unloaded.add(location);
    }

    public List<String> lines(SignLocation location) {
        return lines.get(location);
    }

    public boolean released(SignLocation location) {
        return released.contains(location);
    }

    @Override
    public boolean isSign(SignLocation location) {
        return signs.contains(location) && !unloaded.contains(location);
    }

    @Override
    public Result write(SignLocation location, List<Component> written) {
        if (unloaded.contains(location)) {
            return Result.NOT_LOADED;
        }
        if (!signs.contains(location)) {
            return Result.NOT_A_SIGN;
        }
        lines.put(location, written.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList());
        return Result.WRITTEN;
    }

    @Override
    public void release(SignLocation location) {
        released.add(location);
    }
}
