package org.skysongdev.skysongClaims.storage;

import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.SignLocation;
import org.skysongdev.skysongClaims.sign.SignRepository;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}; share the Claims' writer. */
public final class AsyncSignRepository implements SignRepository {
    private final SignRepository delegate;
    private final BackgroundWriter writer;

    public AsyncSignRepository(SignRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<ClaimSign> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(ClaimSign sign) {
        writer.run("save the sign at " + sign.location(), () -> delegate.save(sign));
    }

    @Override
    public void delete(SignLocation location) {
        writer.run("delete the sign at " + location, () -> delegate.delete(location));
    }
}
