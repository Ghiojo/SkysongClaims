package org.skysongdev.skysongClaims.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimGroup;
import org.skysongdev.skysongClaims.claim.ClaimOwner;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.GroupPermission;
import org.skysongdev.skysongClaims.claim.OwnerCause;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlClaimRepositoryTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Alice");
    private static final CharacterId BOB = new CharacterId(UUID.randomUUID(), "Bob");

    private SqlClaimRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        DatabaseSchema.create(dataSource);
        DatabaseSchema.create(dataSource); // creating twice changes nothing
        repository = new SqlClaimRepository(dataSource);
    }

    private static Claim fullClaim() {
        return Claim.create("hovel_1", "hovel", true, 3,
                        List.of(new ClaimRegion("world", "hovel_1"), new ClaimRegion("world_nether", "cellar")))
                .withDisplayName("<gold>Hovel One")
                .withOwners(List.of(
                        new ClaimOwner(BOB, Instant.ofEpochMilli(2000), OwnerCause.FREE_CLAIM),
                        new ClaimOwner(ALICE, Instant.ofEpochMilli(1000), OwnerCause.INVITE)))
                .withGroups(List.of(
                        new ClaimGroup("family", "Family", Set.of(GroupPermission.BUILD), new LinkedHashSet<>(List.of(BOB, ALICE))),
                        new ClaimGroup("guests", "Guests", Set.of(), Set.of())))
                .withSale(Optional.of("SkysongEconomy"))
                .withFlags(java.util.Map.of("greeting", "Welcome", "farewell", "Bye"));
    }

    @Test
    void savesAndLoadsEverythingInOrder() {
        Claim claim = fullClaim();

        repository.save(claim);

        List<Claim> loaded = repository.loadAll();
        assertEquals(List.of(claim), loaded);
        assertEquals(List.of(BOB, ALICE), List.copyOf(loaded.get(0).groups().get(0).members()));
    }

    @Test
    void savingAgainReplacesTheOldRows() {
        repository.save(fullClaim());
        Claim changed = fullClaim()
                .withRegions(List.of(new ClaimRegion("world", "hovel_1")))
                .withOwners(List.of())
                .withGroups(List.of())
                .withSale(Optional.empty())
                .withFlags(java.util.Map.of("greeting", "Hi"))
                .withType("other");

        repository.save(changed);

        assertEquals(List.of(changed), repository.loadAll());
    }

    @Test
    void deleteRemovesTheClaimAndItsRows() {
        repository.save(fullClaim());
        Claim other = Claim.create("other_1", "other", false, 1, List.of(new ClaimRegion("world", "hovel_1")));

        repository.delete("hovel_1");
        repository.save(other); // the region is free again

        assertEquals(List.of(other), repository.loadAll());
    }

    @Test
    void aRegionCantBeSavedInTwoClaims() {
        repository.save(fullClaim());

        assertThrows(StorageException.class, () -> repository.save(
                Claim.create("thief", "other", false, 1, List.of(new ClaimRegion("world", "hovel_1")))));
        assertEquals(1, repository.loadAll().size());
    }

    @Test
    void deletingAnUnsavedClaimDoesNothing() {
        repository.delete("nothing");

        assertTrue(repository.loadAll().isEmpty());
    }
}
