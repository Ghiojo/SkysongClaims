package org.skysongdev.skysongClaims.storage;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimRegion;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncClaimRepositoryTest {
    private static Claim claim(String id) {
        return Claim.create(id, "other", false, 1, List.of(new ClaimRegion("world", id)));
    }

    @Test
    void writesInOrderAndFinishesThemOnClose() {
        InMemoryClaimRepository memory = new InMemoryClaimRepository();
        BackgroundWriter writer = new BackgroundWriter(Logger.getAnonymousLogger());
        AsyncClaimRepository repository = new AsyncClaimRepository(memory, writer);

        repository.save(claim("a"));
        repository.save(claim("b"));
        repository.delete("a");
        writer.close();

        assertEquals(List.of(claim("b")), memory.loadAll());
    }

    @Test
    void aFailedWriteDoesNotStopLaterOnes() {
        InMemoryClaimRepository memory = new InMemoryClaimRepository();
        BackgroundWriter writer = new BackgroundWriter(Logger.getAnonymousLogger());

        writer.run("fail on purpose", () -> {
            throw new IllegalStateException("boom");
        });
        new AsyncClaimRepository(memory, writer).save(claim("a"));
        writer.close();

        assertTrue(memory.loadAll().contains(claim("a")));
    }
}
