package org.skysongdev.skysongClaims.claim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps Claims in memory only, for tests of the features that save Claims. */
public final class InMemoryClaimRepository implements ClaimRepository {
    private final Map<String, Claim> claims = new LinkedHashMap<>();

    @Override
    public synchronized List<Claim> loadAll() {
        return new ArrayList<>(claims.values());
    }

    @Override
    public synchronized void save(Claim claim) {
        claims.put(claim.id(), claim);
    }

    @Override
    public synchronized void delete(String claimId) {
        claims.remove(claimId);
    }
}
