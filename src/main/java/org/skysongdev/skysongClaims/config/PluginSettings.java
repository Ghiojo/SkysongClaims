package org.skysongdev.skysongClaims.config;

import org.skysongdev.skysongClaims.type.LimitScope;

import java.util.Objects;

/**
 * The plugin-wide settings of {@code config.yml}.
 *
 * @param defaultLimitScope whether limits count per character or per account, for types that don't say
 */
public record PluginSettings(LimitScope defaultLimitScope) {
    /** File name in the data folder and in the jar. */
    public static final String FILE = "config.yml";

    public PluginSettings {
        Objects.requireNonNull(defaultLimitScope, "defaultLimitScope");
    }

    /** @throws ConfigException when a setting has a value the plugin can't use */
    public static PluginSettings parse(ConfigSection root) {
        String raw = root.string("limit-per", LimitScope.CHARACTER.configName());
        LimitScope scope = LimitScope.fromConfig(raw)
                .orElseThrow(() -> root.error("limit-per", "must be 'character' or 'account', not '" + raw + "'"));
        return new PluginSettings(scope);
    }
}
