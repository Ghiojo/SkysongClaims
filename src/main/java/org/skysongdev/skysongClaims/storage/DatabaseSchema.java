package org.skysongdev.skysongClaims.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/**
 * Creates the plugin's tables if they don't exist yet. Every child row is deleted with its
 * Claim ({@code ON DELETE CASCADE}).
 */
public final class DatabaseSchema {
    static final List<String> TABLES = List.of(
            "CREATE TABLE IF NOT EXISTS claims ("
                    + "id VARCHAR(32) PRIMARY KEY NOT NULL, "
                    + "display_name VARCHAR(255) NOT NULL, "
                    + "type_id VARCHAR(32) NOT NULL, "
                    // NULL for a bought Claim, which has no cap on owners.
                    + "slots INT NULL, "
                    + "for_sale_by VARCHAR(64) NULL) ENGINE=InnoDB",
            // A WorldGuard region belongs to one Claim at most, so (world, region_id) is the key.
            "CREATE TABLE IF NOT EXISTS claim_regions ("
                    + "claim_id VARCHAR(32) NOT NULL, "
                    + "world VARCHAR(128) NOT NULL, "
                    + "region_id VARCHAR(128) NOT NULL, "
                    + "position INT NOT NULL, "
                    + "PRIMARY KEY (world, region_id), "
                    + "INDEX (claim_id), "
                    + "FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS claim_owners ("
                    + "claim_id VARCHAR(32) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "since BIGINT NOT NULL, "
                    + "cause VARCHAR(16) NOT NULL, "
                    + "position INT NOT NULL, "
                    + "PRIMARY KEY (claim_id, player_uuid, profile), "
                    + "FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS claim_groups ("
                    + "claim_id VARCHAR(32) NOT NULL, "
                    + "group_id VARCHAR(32) NOT NULL, "
                    + "display_name VARCHAR(255) NOT NULL, "
                    + "permissions VARCHAR(255) NOT NULL, "
                    + "position INT NOT NULL, "
                    + "PRIMARY KEY (claim_id, group_id), "
                    + "FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE) ENGINE=InnoDB",
            "CREATE TABLE IF NOT EXISTS claim_group_members ("
                    + "claim_id VARCHAR(32) NOT NULL, "
                    + "group_id VARCHAR(32) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "profile VARCHAR(64) NOT NULL, "
                    + "position INT NOT NULL, "
                    + "PRIMARY KEY (claim_id, group_id, player_uuid, profile), "
                    + "FOREIGN KEY (claim_id, group_id) REFERENCES claim_groups(claim_id, group_id) "
                    + "ON DELETE CASCADE) ENGINE=InnoDB",
            // The WorldGuard flags owners set, kept here so every region (also ones added later) gets them.
            "CREATE TABLE IF NOT EXISTS claim_flags ("
                    + "claim_id VARCHAR(32) NOT NULL, "
                    + "flag VARCHAR(64) NOT NULL, "
                    + "flag_value VARCHAR(1024) NOT NULL, "
                    + "PRIMARY KEY (claim_id, flag), "
                    + "FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE) ENGINE=InnoDB",
            // price_line is the fourth line another plugin set, as Adventure JSON.
            "CREATE TABLE IF NOT EXISTS claim_signs ("
                    + "claim_id VARCHAR(32) NOT NULL, "
                    + "world VARCHAR(128) NOT NULL, "
                    + "x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, "
                    + "price_line TEXT NULL, "
                    + "PRIMARY KEY (world, x, y, z), "
                    + "INDEX (claim_id), "
                    + "FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE) ENGINE=InnoDB");

    private DatabaseSchema() {
    }

    /** @throws StorageException when a table can't be created */
    public static void create(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String table : TABLES) {
                statement.execute(table);
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to create the SkysongClaims tables", e);
        }
    }
}
