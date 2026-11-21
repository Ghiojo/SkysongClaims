package org.skysongdev.skysongClaims.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Builds the HikariCP connection pool. The pool checks connections before handing them out
 * and replaces ones MySQL has dropped, so a database restart doesn't break the plugin.
 */
public final class ConnectionPool {
    static final int MAXIMUM_POOL_SIZE = 2;

    private ConnectionPool() {
    }

    public static HikariDataSource create(DatabaseSettings settings) {
        return new HikariDataSource(config(settings));
    }

    static HikariConfig config(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("SkysongClaims");
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        // Claims load once at start-up and writes go through one background thread.
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        // The plugin can't work without MySQL, so opening the pool fails at once without it.
        config.setInitializationFailTimeout(1);
        return config;
    }
}
