package org.skysongdev.skysongClaims.sign;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps signs in memory only, for tests of the features that save them. */
public final class InMemorySignRepository implements SignRepository {
    private final Map<SignLocation, ClaimSign> signs = new LinkedHashMap<>();

    @Override
    public synchronized List<ClaimSign> loadAll() {
        return new ArrayList<>(signs.values());
    }

    @Override
    public synchronized void save(ClaimSign sign) {
        signs.put(sign.location(), sign);
    }

    @Override
    public synchronized void delete(SignLocation location) {
        signs.remove(location);
    }
}
