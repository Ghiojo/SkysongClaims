package org.skysongdev.skysongClaims.storage;

import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRepository;
import org.skysongdev.skysongstats.api.CharacterId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/** Saves Claims in the MySQL tables of {@link DatabaseSchema}. Blocking: wrap it in {@link AsyncClaimRepository}. */
public final class SqlClaimRepository implements ClaimRepository {
    private static final String UPSERT_CLAIM = "INSERT INTO claims (id, display_name, type_id, free, max_owners, for_sale_by) "
            + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), "
            + "type_id = VALUES(type_id), free = VALUES(free), max_owners = VALUES(max_owners), "
            + "for_sale_by = VALUES(for_sale_by)";

    private final DataSource dataSource;

    public SqlClaimRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Claim> loadAll() {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, List<ClaimRegion>> regions = loadRegions(connection);
            Map<String, List<ClaimOwner>> owners = loadOwners(connection);
            Map<String, List<ClaimGroup>> groups = loadGroups(connection);
            Map<String, Map<String, String>> flags = loadFlags(connection);
            List<Claim> claims = new ArrayList<>();
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id, display_name, type_id, free, max_owners, for_sale_by FROM claims ORDER BY id");
                 ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    String id = rows.getString("id");
                    claims.add(new Claim(id, rows.getString("display_name"), rows.getString("type_id"),
                            rows.getBoolean("free"), rows.getInt("max_owners"),
                            regions.getOrDefault(id, List.of()), owners.getOrDefault(id, List.of()),
                            groups.getOrDefault(id, List.of()), Optional.ofNullable(rows.getString("for_sale_by")),
                            flags.getOrDefault(id, Map.of())));
                }
            }
            return claims;
        } catch (SQLException | IllegalArgumentException e) {
            throw new StorageException("Failed to load the claims", e);
        }
    }

    private static Map<String, List<ClaimRegion>> loadRegions(Connection connection) throws SQLException {
        Map<String, List<ClaimRegion>> regions = new HashMap<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT claim_id, world, region_id FROM claim_regions ORDER BY claim_id, position");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                regions.computeIfAbsent(rows.getString("claim_id"), id -> new ArrayList<>())
                        .add(new ClaimRegion(rows.getString("world"), rows.getString("region_id")));
            }
        }
        return regions;
    }

    private static Map<String, List<ClaimOwner>> loadOwners(Connection connection) throws SQLException {
        Map<String, List<ClaimOwner>> owners = new HashMap<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT claim_id, player_uuid, profile, since, cause FROM claim_owners ORDER BY claim_id, position");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                owners.computeIfAbsent(rows.getString("claim_id"), id -> new ArrayList<>()).add(new ClaimOwner(
                        character(rows), Instant.ofEpochMilli(rows.getLong("since")),
                        OwnerCause.valueOf(rows.getString("cause"))));
            }
        }
        return owners;
    }

    private static Map<String, List<ClaimGroup>> loadGroups(Connection connection) throws SQLException {
        Map<String, Map<String, Set<CharacterId>>> members = new HashMap<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT claim_id, group_id, player_uuid, profile FROM claim_group_members "
                        + "ORDER BY claim_id, group_id, position");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                members.computeIfAbsent(rows.getString("claim_id"), id -> new HashMap<>())
                        .computeIfAbsent(rows.getString("group_id"), id -> new LinkedHashSet<>())
                        .add(character(rows));
            }
        }
        Map<String, List<ClaimGroup>> groups = new HashMap<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT claim_id, group_id, display_name, permissions FROM claim_groups ORDER BY claim_id, position");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                String claimId = rows.getString("claim_id");
                String groupId = rows.getString("group_id");
                groups.computeIfAbsent(claimId, id -> new ArrayList<>()).add(new ClaimGroup(groupId,
                        rows.getString("display_name"), permissions(rows.getString("permissions")),
                        members.getOrDefault(claimId, Map.of()).getOrDefault(groupId, Set.of())));
            }
        }
        return groups;
    }

    private static Map<String, Map<String, String>> loadFlags(Connection connection) throws SQLException {
        Map<String, Map<String, String>> flags = new HashMap<>();
        try (PreparedStatement select = connection.prepareStatement("SELECT claim_id, flag, flag_value FROM claim_flags");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                flags.computeIfAbsent(rows.getString("claim_id"), id -> new HashMap<>())
                        .put(rows.getString("flag"), rows.getString("flag_value"));
            }
        }
        return flags;
    }

    private static CharacterId character(ResultSet rows) throws SQLException {
        return new CharacterId(UUID.fromString(rows.getString("player_uuid")), rows.getString("profile"));
    }

    private static Set<GroupPermission> permissions(String stored) {
        Set<GroupPermission> permissions = EnumSet.noneOf(GroupPermission.class);
        for (String name : stored.split(",")) {
            if (!name.isBlank()) {
                permissions.add(GroupPermission.valueOf(name.trim()));
            }
        }
        return permissions;
    }

    private static String permissions(Set<GroupPermission> permissions) {
        return permissions.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public void save(Claim claim) {
        SqlTransaction.run(dataSource, "Failed to save claim " + claim.id(), connection -> {
            try (PreparedStatement upsert = connection.prepareStatement(UPSERT_CLAIM)) {
                upsert.setString(1, claim.id());
                upsert.setString(2, claim.displayName());
                upsert.setString(3, claim.typeId());
                upsert.setBoolean(4, claim.free());
                upsert.setInt(5, claim.maxOwners());
                upsert.setString(6, claim.saleSetBy().orElse(null));
                upsert.executeUpdate();
            }
            // Children are replaced whole; deleting a group also deletes its members.
            for (String table : List.of("claim_regions", "claim_owners", "claim_groups", "claim_flags")) {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE claim_id = ?")) {
                    delete.setString(1, claim.id());
                    delete.executeUpdate();
                }
            }
            insertRegions(connection, claim);
            insertOwners(connection, claim);
            insertGroups(connection, claim);
            insertFlags(connection, claim);
        });
    }

    private static void insertRegions(Connection connection, Claim claim) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO claim_regions (claim_id, world, region_id, position) VALUES (?, ?, ?, ?)")) {
            int position = 0;
            for (ClaimRegion region : claim.regions()) {
                insert.setString(1, claim.id());
                insert.setString(2, region.world());
                insert.setString(3, region.regionId());
                insert.setInt(4, position++);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void insertOwners(Connection connection, Claim claim) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO claim_owners (claim_id, player_uuid, profile, since, cause, position) VALUES (?, ?, ?, ?, ?, ?)")) {
            int position = 0;
            for (ClaimOwner owner : claim.owners()) {
                insert.setString(1, claim.id());
                insert.setString(2, owner.character().player().toString());
                insert.setString(3, owner.character().profile());
                insert.setLong(4, owner.since().toEpochMilli());
                insert.setString(5, owner.cause().name());
                insert.setInt(6, position++);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void insertGroups(Connection connection, Claim claim) throws SQLException {
        try (PreparedStatement groups = connection.prepareStatement(
                "INSERT INTO claim_groups (claim_id, group_id, display_name, permissions, position) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement members = connection.prepareStatement(
                     "INSERT INTO claim_group_members (claim_id, group_id, player_uuid, profile, position) VALUES (?, ?, ?, ?, ?)")) {
            int position = 0;
            for (ClaimGroup group : claim.groups()) {
                groups.setString(1, claim.id());
                groups.setString(2, group.id());
                groups.setString(3, group.displayName());
                groups.setString(4, permissions(group.permissions()));
                groups.setInt(5, position++);
                groups.addBatch();
                int memberPosition = 0;
                for (CharacterId member : group.members()) {
                    members.setString(1, claim.id());
                    members.setString(2, group.id());
                    members.setString(3, member.player().toString());
                    members.setString(4, member.profile());
                    members.setInt(5, memberPosition++);
                    members.addBatch();
                }
            }
            groups.executeBatch();
            members.executeBatch();
        }
    }

    private static void insertFlags(Connection connection, Claim claim) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO claim_flags (claim_id, flag, flag_value) VALUES (?, ?, ?)")) {
            for (Map.Entry<String, String> flag : claim.flags().entrySet()) {
                insert.setString(1, claim.id());
                insert.setString(2, flag.getKey());
                insert.setString(3, flag.getValue());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    @Override
    public void delete(String claimId) {
        SqlTransaction.run(dataSource, "Failed to delete claim " + claimId, connection -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM claims WHERE id = ?")) {
                delete.setString(1, claimId);
                delete.executeUpdate();
            }
        });
    }
}
