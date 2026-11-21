package org.skysongdev.skysongClaims.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.SignLocation;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlSignRepositoryTest {
    private static final SignLocation BOARD = new SignLocation("world", 10, 64, -3);

    private SqlClaimRepository claims;
    private SqlSignRepository signs;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        DatabaseSchema.create(dataSource);
        claims = new SqlClaimRepository(dataSource);
        signs = new SqlSignRepository(dataSource);
        claims.save(Claim.create("house", "other", OptionalInt.empty(), List.of(new ClaimRegion("world", "house"))));
    }

    @Test
    void savesReplacesAndDeletes() {
        signs.save(new ClaimSign("house", BOARD, Optional.empty()));
        ClaimSign priced = new ClaimSign("house", BOARD, Optional.of("{\"text\":\"500 gold\"}"));

        signs.save(priced);
        assertEquals(List.of(priced), signs.loadAll());

        signs.delete(BOARD);
        assertTrue(signs.loadAll().isEmpty());
    }

    @Test
    void deletingTheClaimDeletesItsSigns() {
        signs.save(new ClaimSign("house", BOARD, Optional.empty()));

        claims.delete("house");

        assertTrue(signs.loadAll().isEmpty());
    }
}
