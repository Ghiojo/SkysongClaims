package org.skysongdev.skysongClaims.config;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.storage.DatabaseSettings;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.type.LimitScope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigHolderTest {
    private static LoadedConfig config(LimitScope scope) {
        return new LoadedConfig(new PluginSettings(new DatabaseSettings("localhost", 3306, "root", "", "skysong"), scope, java.time.Duration.ofMinutes(5),
                new org.skysongdev.skysongClaims.group.GroupLimits(5, 0), java.util.List.of()), ClaimTypeRegistry.empty(), TestMessages.catalog());
    }

    /** Hands out the given configs in order, throwing for a {@code null} entry. */
    private static Supplier<LoadedConfig> loads(LoadedConfig... configs) {
        List<LoadedConfig> queue = new ArrayList<>(java.util.Arrays.asList(configs));
        return () -> {
            LoadedConfig next = queue.remove(0);
            if (next == null) {
                throw new ConfigException("types.yml: broken");
            }
            return next;
        };
    }

    @Test
    void loadsOnCreation() {
        LoadedConfig first = config(LimitScope.CHARACTER);

        assertSame(first, new ConfigHolder(() -> first).current());
    }

    @Test
    void reloadSwitchesAndTellsListeners() {
        LoadedConfig second = config(LimitScope.ACCOUNT);
        ConfigHolder holder = new ConfigHolder(loads(config(LimitScope.CHARACTER), second));
        List<LoadedConfig> told = new ArrayList<>();
        holder.onReload(told::add);

        holder.reload();

        assertSame(second, holder.current());
        assertEquals(List.of(second), told);
    }

    @Test
    void aBrokenReloadKeepsTheOldConfig() {
        LoadedConfig first = config(LimitScope.CHARACTER);
        ConfigHolder holder = new ConfigHolder(loads(first, null));
        List<LoadedConfig> told = new ArrayList<>();
        holder.onReload(told::add);

        assertThrows(ConfigException.class, holder::reload);

        assertSame(first, holder.current());
        assertTrue(told.isEmpty());
    }
}
