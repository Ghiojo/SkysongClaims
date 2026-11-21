package org.skysongdev.skysongClaims.config;

import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;

import java.util.Objects;

/**
 * One consistent load of every config file. Files are read together, so a reload never mixes
 * a new {@code config.yml} with an old {@code types.yml}.
 *
 * @param settings the plugin-wide settings of {@code config.yml}
 * @param types    the Claim types of {@code types.yml}
 */
public record LoadedConfig(PluginSettings settings, ClaimTypeRegistry types) {
    public LoadedConfig {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(types, "types");
    }
}
