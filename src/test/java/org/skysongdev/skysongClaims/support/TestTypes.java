package org.skysongdev.skysongClaims.support;

import org.skysongdev.skysongClaims.api.LimitScope;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.type.ClaimTypesParser;

import java.util.List;
import java.util.Map;

/** Claim types for tests: a Free {@code hovel} with 2 slots (limit 1) and a bought {@code other} (limit 3). */
public final class TestTypes {
    public static final ClaimType HOVEL = new ClaimType("hovel", "<gold>Hovel", 1, LimitScope.CHARACTER, 2, true,
            ClaimTypesParser.DEFAULT_OPEN_USE, Map.of());
    public static final ClaimType OTHER = new ClaimType("other", "Other", 3, LimitScope.CHARACTER, 1, false,
            ClaimTypesParser.DEFAULT_OPEN_USE, Map.of());

    private TestTypes() {
    }

    public static ClaimTypeRegistry registry() {
        return new ClaimTypeRegistry(List.of(HOVEL, OTHER));
    }
}
