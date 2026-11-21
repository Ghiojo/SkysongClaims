package org.skysongdev.skysongClaims.apiimpl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainThreadTest {

    @Test
    void allowsTheMainThread() {
        assertDoesNotThrow(() -> new MainThread(() -> true).check());
    }

    @Test
    void refusesOtherThreads() {
        assertThrows(IllegalStateException.class, () -> new MainThread(() -> false).check());
    }
}
