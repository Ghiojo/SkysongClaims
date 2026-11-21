package org.skysongdev.skysongClaims.config;

import org.skysongdev.skysongClaims.messaging.MessageCatalog;
import org.skysongdev.skysongClaims.type.ClaimTypesParser;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Reads every config file of the plugin into one {@link LoadedConfig}. */
public final class ConfigLoader {
    private final YamlFiles files;
    private final Function<String, ConfigSection> bundled;
    private final Consumer<String> warnings;

    /**
     * @param files    reads the files from the data folder, creating missing ones from the jar
     * @param bundled  reads a default file from the jar, for messages the server's file lacks
     * @param warnings receives one message per problem that was skipped
     */
    public ConfigLoader(YamlFiles files, Function<String, ConfigSection> bundled, Consumer<String> warnings) {
        this.files = Objects.requireNonNull(files, "files");
        this.bundled = Objects.requireNonNull(bundled, "bundled");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** @throws ConfigException when a file can't be read or holds a value the plugin can't use */
    public LoadedConfig load() {
        PluginSettings settings = PluginSettings.parse(files.load(PluginSettings.FILE));
        ClaimTypesParser types = new ClaimTypesParser(settings.defaultLimitScope(), warnings);
        MessageCatalog messages = MessageCatalog.of(bundled.apply(MessageCatalog.FILE),
                files.load(MessageCatalog.FILE), warnings);
        return new LoadedConfig(settings, types.parse(files.load(ClaimTypesParser.FILE)), messages);
    }
}
