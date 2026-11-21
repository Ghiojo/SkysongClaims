package org.skysongdev.skysongClaims.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Runs a group of statements as one transaction: all of them are saved, or none. */
final class SqlTransaction {
    /** Statements to run inside the transaction. */
    @FunctionalInterface
    interface Work {
        void run(Connection connection) throws SQLException;
    }

    private SqlTransaction() {
    }

    /**
     * @param failure what to say when it fails, e.g. {@code "Failed to save claim hovel_1"}
     * @throws StorageException when a statement fails; nothing is saved then
     */
    static void run(DataSource dataSource, String failure, Work work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new StorageException(failure, e);
        }
    }
}
