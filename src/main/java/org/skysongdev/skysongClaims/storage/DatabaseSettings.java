package org.skysongdev.skysongClaims.storage;

import org.skysongdev.skysongClaims.config.ConfigSection;

import java.util.Objects;

/** Connection settings, from the {@code database} section of {@code config.yml}. */
public record DatabaseSettings(String host, int port, String user, String password, String databaseName) {
    public DatabaseSettings {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(databaseName, "databaseName");
    }

    /** Reads the section; every key has the default of a local MySQL server. */
    public static DatabaseSettings parse(ConfigSection section) {
        return new DatabaseSettings(
                section.string("host", "localhost"),
                section.integer("port", 3306),
                section.string("user", "root"),
                section.string("password", ""),
                section.string("database_name", "skysong"));
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
    }

    /** Keeps the password out of logs and stack traces. */
    @Override
    public String toString() {
        return "DatabaseSettings[" + user + "@" + host + ":" + port + "/" + databaseName + "]";
    }
}
