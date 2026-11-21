package org.skysongdev.skysongClaims.storage;

import com.zaxxer.hikari.HikariDataSource;
import org.skysongdev.skysongClaims.claim.ClaimRepository;

import java.util.logging.Logger;

/**
 * The open database: its connection pool, the background writer and the repositories on
 * top. Closing it saves what is still queued, then closes the pool.
 */
public final class ClaimStorage implements AutoCloseable {
    private final HikariDataSource pool;
    private final BackgroundWriter writer;
    private final ClaimRepository claims;

    private ClaimStorage(HikariDataSource pool, BackgroundWriter writer) {
        this.pool = pool;
        this.writer = writer;
        this.claims = new AsyncClaimRepository(new SqlClaimRepository(pool), writer);
    }

    /**
     * Connects and creates the tables that are missing.
     *
     * @throws StorageException when the database can't be reached or the tables can't be made
     * @throws RuntimeException from HikariCP when the first connection fails
     */
    public static ClaimStorage open(DatabaseSettings settings, Logger logger) {
        HikariDataSource pool = ConnectionPool.create(settings);
        try {
            DatabaseSchema.create(pool);
        } catch (RuntimeException e) {
            pool.close();
            throw e;
        }
        return new ClaimStorage(pool, new BackgroundWriter(logger));
    }

    /** @return the Claims, saved in the background */
    public ClaimRepository claims() {
        return claims;
    }

    @Override
    public void close() {
        writer.close();
        pool.close();
    }
}
