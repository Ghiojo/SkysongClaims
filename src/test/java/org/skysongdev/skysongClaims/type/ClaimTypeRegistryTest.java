package org.skysongdev.skysongClaims.type;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.LimitScope;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimTypeRegistryTest {
    private static ClaimType type(String id) {
        return new ClaimType(id, id, 1, LimitScope.CHARACTER, 1, false, List.of(), Map.of());
    }

    @Test
    void refusesDuplicateIds() {
        assertThrows(IllegalArgumentException.class, () -> new ClaimTypeRegistry(List.of(type("a"), type("a"))));
    }

    @Test
    void emptyHasNoTypes() {
        assertTrue(ClaimTypeRegistry.empty().all().isEmpty());
    }

    @Test
    void extraCantBeChanged() {
        ClaimType type = new ClaimType("a", "A", 1, LimitScope.CHARACTER, 1, false, List.of(), Map.of("k", 1));

        assertThrows(UnsupportedOperationException.class, () -> type.extra().put("k", 2));
    }
}
