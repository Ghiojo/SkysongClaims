package org.skysongdev.skysongClaims.storage;

import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRepository;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncClaimRepository implements ClaimRepository {
    private final ClaimRepository delegate;
    private final BackgroundWriter writer;

    public AsyncClaimRepository(ClaimRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Claim> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(Claim claim) {
        writer.run("save claim " + claim.id(), () -> delegate.save(claim));
    }

    @Override
    public void delete(String claimId) {
        writer.run("delete claim " + claimId, () -> delegate.delete(claimId));
    }
}
