package org.skysongdev.skysongClaims.type;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.LimitScope;
import org.skysongdev.skysongClaims.config.ConfigException;
import org.skysongdev.skysongClaims.config.ConfigSection;
import org.skysongdev.skysongClaims.config.YamlFiles;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimTypesParserTest {
    private final List<String> warnings = new ArrayList<>();
    private final ClaimTypesParser parser = new ClaimTypesParser(LimitScope.CHARACTER, warnings::add);

    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("types.yml", new StringReader(text));
    }

    private ClaimType only(String text) {
        List<ClaimType> types = parser.parse(yaml(text)).all();
        assertEquals(1, types.size());
        return types.get(0);
    }

    @Test
    void readsAFreeClaimType() {
        ClaimType hovel = only("""
                types:
                  hovel:
                    display-name: "<gold>Hovel"
                    limit: 1
                    free-claim: { default-max-owners: 2 }
                    open-use: [use, chest-access]
                    extra: { price-modifier: 10, tags: [cheap] }
                """);

        assertEquals("hovel", hovel.id());
        assertEquals("<gold>Hovel", hovel.displayName());
        assertEquals(1, hovel.limit());
        assertEquals(LimitScope.CHARACTER, hovel.limitScope());
        assertTrue(hovel.freeClaim());
        assertEquals(OptionalInt.of(2), hovel.freeClaimSlots());
        assertEquals(List.of("use", "chest-access"), hovel.openUse());
        assertEquals(Map.of("price-modifier", 10, "tags", List.of("cheap")), hovel.extra());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void aTypeWithoutFreeClaimIsBoughtWithNoCapOnOwners() {
        ClaimType other = only("types: { other: { display-name: Other, limit: 3 } }");

        assertFalse(other.freeClaim());
        assertEquals(OptionalInt.empty(), other.freeClaimSlots());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void maxOwnersIsIgnoredWithAWarning() {
        assertEquals(OptionalInt.empty(),
                only("types: { manor: { display-name: Manor, limit: 1, max-owners: 4 } }").freeClaimSlots());
        ClaimType hovel = only("types: { hovel: { display-name: H, limit: 1, max-owners: 5, free-claim: { default-max-owners: 2 } } }");

        assertEquals(OptionalInt.of(2), hovel.freeClaimSlots());
        assertEquals(2, warnings.size());
    }

    @Test
    void aTypeCanCountItsLimitPerAccount() {
        assertEquals(LimitScope.ACCOUNT,
                only("types: { manor: { display-name: M, limit: 1, limit-per: account } }").limitScope());
    }

    @Test
    void typesWithoutLimitPerUseThePluginDefault() {
        ClaimTypesParser accountParser = new ClaimTypesParser(LimitScope.ACCOUNT, warnings::add);

        ClaimType type = accountParser.parse(yaml("types: { other: { display-name: O, limit: 1 } }")).all().get(0);

        assertEquals(LimitScope.ACCOUNT, type.limitScope());
    }

    @Test
    void typesWithoutOpenUseOpenTheStandardFlags() {
        assertEquals(ClaimTypesParser.DEFAULT_OPEN_USE,
                only("types: { other: { display-name: O, limit: 1 } }").openUse());
    }

    @Test
    void anEmptyOpenUseListOpensNothing() {
        assertEquals(List.of(), only("types: { other: { display-name: O, limit: 1, open-use: [] } }").openUse());
    }

    @Test
    void buildCanNeverBeOpened() {
        ClaimType type = only("types: { other: { display-name: O, limit: 1, open-use: [use, build, 'bad flag'] } }");

        assertEquals(List.of("use"), type.openUse());
        assertEquals(2, warnings.size());
    }

    @Test
    void keepsFileOrder() {
        ClaimTypeRegistry registry = parser.parse(yaml("""
                types:
                  b: { display-name: B, limit: 1 }
                  a: { display-name: A, limit: 1 }
                """));

        assertEquals(List.of("b", "a"), registry.all().stream().map(ClaimType::id).toList());
        assertTrue(registry.find("a").isPresent());
        assertTrue(registry.find("c").isEmpty());
    }

    @Test
    void warnsWhenThereAreNoTypes() {
        assertTrue(parser.parse(yaml("types: {}")).all().isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void refusesBadValues() {
        assertRefused("types: { Bad: { display-name: B, limit: 1 } }", "Bad");
        assertRefused("types: { a: { limit: 1 } }", "display-name");
        assertRefused("types: { a: { display-name: A } }", "limit");
        assertRefused("types: { a: { display-name: A, limit: -1 } }", "limit");
        assertRefused("types: { a: { display-name: A, limit: 1, limit-per: guild } }", "limit-per");
        assertRefused("types: { a: { display-name: A, limit: 1, free-claim: { default-max-owners: 0 } } }", "default-max-owners");
        assertRefused("types: { a: { display-name: A, limit: 1, extra: [x] } }", "extra");
    }

    private void assertRefused(String text, String key) {
        ConfigException refusal = assertThrows(ConfigException.class, () -> parser.parse(yaml(text)));
        assertTrue(refusal.getMessage().contains(key), refusal.getMessage());
    }
}
