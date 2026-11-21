package org.skysongdev.skysongClaims.claim;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.messaging.RefusedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimStoreTest {
    private final InMemoryClaimRepository repository = new InMemoryClaimRepository();
    private final ClaimStore store = new ClaimStore(new ClaimState(), repository);
    private final List<ClaimChange> changes = new ArrayList<>();

    private static Claim claim(String name) {
        return Claim.create("a", "other", false, 1, List.of(new ClaimRegion("world", "a"))).withDisplayName(name);
    }

    @Test
    void savesThenTellsObserversWithBeforeAndAfter() {
        store.observe(change -> {
            assertEquals(change.after(), repository.loadAll().stream().findFirst(), "saved before observers hear");
            changes.add(change);
        });

        store.save(claim("one"), ChangeCause.COMMAND);
        store.save(claim("two"), ChangeCause.API);

        assertEquals(2, changes.size());
        assertEquals(Optional.of(claim("one")), changes.get(1).before());
        assertEquals(ChangeCause.API, changes.get(1).cause());
        assertEquals("a", changes.get(1).claimId());
    }

    @Test
    void savingAnUnchangedClaimDoesNothing() {
        store.save(claim("one"), ChangeCause.COMMAND);
        store.observe(changes::add);

        store.save(claim("one"), ChangeCause.COMMAND);

        assertTrue(changes.isEmpty());
    }

    @Test
    void deletingAMissingClaimDoesNothing() {
        store.observe(changes::add);

        store.delete("a", ChangeCause.COMMAND);

        assertTrue(changes.isEmpty());
        assertThrows(RefusedException.class, () -> store.require("a"));
    }

    @Test
    void aChangeNeedsAClaim() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClaimChange(Optional.empty(), Optional.empty(), ChangeCause.COMMAND));
    }
}
