package org.skysongdev.skysongClaims.storage;

import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.SignLocation;
import org.skysongdev.skysongClaims.sign.SignRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Saves Claim signs in the {@code claim_signs} table. Blocking: wrap it in {@link AsyncSignRepository}. */
public final class SqlSignRepository implements SignRepository {
    private final DataSource dataSource;

    public SqlSignRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<ClaimSign> loadAll() {
        List<ClaimSign> signs = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT claim_id, world, x, y, z, price_line FROM claim_signs ORDER BY claim_id, world, x, y, z");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                signs.add(new ClaimSign(rows.getString("claim_id"),
                        new SignLocation(rows.getString("world"), rows.getInt("x"), rows.getInt("y"), rows.getInt("z")),
                        Optional.ofNullable(rows.getString("price_line"))));
            }
            return signs;
        } catch (SQLException e) {
            throw new StorageException("Failed to load the claim signs", e);
        }
    }

    @Override
    public void save(ClaimSign sign) {
        SqlTransaction.run(dataSource, "Failed to save the sign at " + sign.location(), connection -> {
            delete(connection, sign.location());
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO claim_signs (claim_id, world, x, y, z, price_line) VALUES (?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, sign.claimId());
                insert.setString(2, sign.location().world());
                insert.setInt(3, sign.location().x());
                insert.setInt(4, sign.location().y());
                insert.setInt(5, sign.location().z());
                insert.setString(6, sign.priceLine().orElse(null));
                insert.executeUpdate();
            }
        });
    }

    @Override
    public void delete(SignLocation location) {
        SqlTransaction.run(dataSource, "Failed to delete the sign at " + location, connection -> delete(connection, location));
    }

    private static void delete(Connection connection, SignLocation location) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM claim_signs WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            delete.setString(1, location.world());
            delete.setInt(2, location.x());
            delete.setInt(3, location.y());
            delete.setInt(4, location.z());
            delete.executeUpdate();
        }
    }
}
