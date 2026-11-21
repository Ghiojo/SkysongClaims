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

    @Test
    void invitesExpireAfterFiveMinutesByDefault() {
        assertEquals(java.time.Duration.ofMinutes(5), parse("").inviteExpiry());
    }

    @Test
    void readsInviteExpiry() {
        assertEquals(java.time.Duration.ofSeconds(30), parse("invite-expiry: 30s").inviteExpiry());
        assertEquals(java.time.Duration.ofHours(1), parse("invite-expiry: 1h").inviteExpiry());
        assertThrows(ConfigException.class, () -> parse("invite-expiry: soon"));
        assertThrows(ConfigException.class, () -> parse("invite-expiry: 0m"));
    }

    @Test
    void groupLimitsDefaultToFiveGroupsAndNoMemberLimit() {
        assertEquals(new org.skysongdev.skysongClaims.group.GroupLimits(5, 0), parse("").groupLimits());
        assertEquals(new org.skysongdev.skysongClaims.group.GroupLimits(2, 10),
                parse("{max-groups-per-claim: 2, max-members-per-group: 10}").groupLimits());
        assertThrows(ConfigException.class, () -> parse("max-groups-per-claim: -1"));
    }

    @Test
    void ownersSetGreetingAndFarewellByDefault() {
        assertEquals(java.util.List.of("greeting", "farewell"), parse("").ownerFlags());
        assertEquals(java.util.List.of("greeting-title"), parse("owner-flags: [Greeting-Title]").ownerFlags());
    }
}
