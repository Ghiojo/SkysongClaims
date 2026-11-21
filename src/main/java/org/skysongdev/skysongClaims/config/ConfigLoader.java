package org.skysongdev.skysongClaims.config;

import org.skysongdev.skysongClaims.type.ClaimTypesParser;

import java.util.Objects;
import java.util.function.Consumer;

/** Reads every config file of the plugin into one {@link LoadedConfig}. */
public final class ConfigLoader {
    private final YamlFiles files;
    private final Consumer<String> warnings;

    /**
     * @param files    reads the files from the data folder, creating missing ones from the jar
     * @param warnings receives one message per problem that was skipped
     */
    public ConfigLoader(YamlFiles files, Consumer<String> warnings) {
        this.files = Objects.requireNonNull(files, "files");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** @throws ConfigException when a file can't be read or holds a value the plugin can't use */
    public LoadedConfig load() {
        PluginSettings settings = PluginSettings.parse(files.load(PluginSettings.FILE));
        ClaimTypesParser types = new ClaimTypesParser(settings.defaultLimitScope(), warnings);
        return new LoadedConfig(settings, types.parse(files.load(ClaimTypesParser.FILE)));
    }
}
