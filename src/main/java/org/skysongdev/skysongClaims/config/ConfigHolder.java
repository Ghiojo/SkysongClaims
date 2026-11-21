package org.skysongdev.skysongClaims.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The config the plugin runs with. A reload reads every file again and only replaces the
 * running config when all of them are valid, so a typo never leaves the server half
 * configured. Features that keep something derived from the config register a listener.
 */
public final class ConfigHolder {
    private final Supplier<LoadedConfig> loader;
    private final List<Consumer<LoadedConfig>> listeners = new ArrayList<>();
    private LoadedConfig current;

    /**
     * Loads the config for the first time.
     *
     * @param loader reads every file, e.g. {@code configLoader::load}
     * @throws ConfigException when the config is not valid
     */
    public ConfigHolder(Supplier<LoadedConfig> loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.current = Objects.requireNonNull(loader.get(), "loaded config");
    }

    /** @return the config in use */
    public LoadedConfig current() {
        return current;
    }

    /** Calls {@code listener} after every successful reload, with the new config. */
    public void onReload(Consumer<LoadedConfig> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Reads every file again and switches to it, then tells the listeners.
     *
     * @return the new config
     * @throws ConfigException when the new config is not valid; the old one stays in use
     */
    public LoadedConfig reload() {
        LoadedConfig loaded = Objects.requireNonNull(loader.get(), "loaded config");
        current = loaded;
        listeners.forEach(listener -> listener.accept(loaded));
        return loaded;
    }
}
