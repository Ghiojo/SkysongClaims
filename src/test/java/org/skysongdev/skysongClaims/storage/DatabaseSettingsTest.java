package org.skysongdev.skysongClaims.storage;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.config.YamlFiles;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DatabaseSettingsTest {
    private static DatabaseSettings parse(String text) {
        return DatabaseSettings.parse(YamlFiles.parse("config.yml", new StringReader(text)).section("database"));
    }

    @Test
    void defaultsToALocalServer() {
        assertEquals(new DatabaseSettings("localhost", 3306, "root", "", "skysong"), parse(""));
    }

    @Test
    void readsEveryKey() {
        DatabaseSettings settings = parse("database: { host: db, port: 3307, user: claims, password: secret, database_name: s }");

        assertEquals("jdbc:mysql://db:3307/s", settings.jdbcUrl());
        assertFalse(settings.toString().contains("secret"));
    }

    @Test
    void thePoolUsesTheSettings() {
        HikariConfig config = ConnectionPool.config(parse("database: { user: claims, password: secret }"));

        assertEquals("jdbc:mysql://localhost:3306/skysong", config.getJdbcUrl());
        assertEquals("claims", config.getUsername());
        assertEquals(ConnectionPool.MAXIMUM_POOL_SIZE, config.getMaximumPoolSize());
    }
}
