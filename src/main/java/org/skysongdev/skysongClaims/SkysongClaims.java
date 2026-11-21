package org.skysongdev.skysongClaims;

import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongClaims.apiimpl.ApiRegistration;
import org.skysongdev.skysongClaims.apiimpl.SkysongClaimsApiImpl;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.config.ConfigException;
import org.skysongdev.skysongClaims.config.ConfigHolder;
import org.skysongdev.skysongClaims.config.ConfigLoader;
import org.skysongdev.skysongClaims.config.YamlFiles;
import org.skysongdev.skysongClaims.storage.ClaimStorage;

import java.util.List;
import java.util.logging.Level;

/**
 * The plugin class. It only builds the features and connects them; every rule lives in the
 * feature classes.
 */
public final class SkysongClaims extends JavaPlugin {
    private ClaimStorage storage;
    private ApiRegistration api;

    @Override
    public void onEnable() {
        ConfigHolder config;
        try {
            YamlFiles files = new YamlFiles(getDataFolder().toPath(), path -> saveResource(path, false));
            config = new ConfigHolder(new ConfigLoader(files, getLogger()::warning)::load);
        } catch (ConfigException e) {
            disable("its config is not valid: " + e.getMessage(), null);
            return;
        }
        getLogger().info("Loaded " + config.current().types().all().size() + " claim types");

        List<Claim> claims;
        try {
            storage = ClaimStorage.open(config.current().settings().database(), getLogger());
            claims = storage.claims().loadAll();
        } catch (RuntimeException e) {
            disable("it can't use its database (" + config.current().settings().database() + ")", e);
            return;
        }
        getLogger().info("Loaded " + claims.size() + " claims");

        // The API is offered last, once everything behind it is running.
        api = new ApiRegistration(getServer().getServicesManager(), new SkysongClaimsApiImpl());
        api.register(this);
    }

    private void disable(String reason, Throwable cause) {
        getLogger().log(Level.SEVERE, "SkysongClaims is disabled because " + reason, cause);
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        // Taken back first, so no plugin calls into features that are shutting down.
        if (api != null) {
            api.unregister();
        }
        // Last, so every change made while shutting down is still saved.
        if (storage != null) {
            storage.close();
            storage = null;
        }
    }
}
