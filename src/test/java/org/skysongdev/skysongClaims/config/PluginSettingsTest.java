package org.skysongdev.skysongClaims.config;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.type.LimitScope;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginSettingsTest {
    private static PluginSettings parse(String text) {
        return PluginSettings.parse(YamlFiles.parse("config.yml", new StringReader(text)));
    }

    @Test
    void limitsCountPerCharacterByDefault() {
        assertEquals(LimitScope.CHARACTER, parse("").defaultLimitScope());
    }

    @Test
    void readsLimitPer() {
        assertEquals(LimitScope.ACCOUNT, parse("limit-per: Account").defaultLimitScope());
    }

    @Test
    void refusesAnUnknownLimitPer() {
        assertThrows(ConfigException.class, () -> parse("limit-per: town"));
    }
}
