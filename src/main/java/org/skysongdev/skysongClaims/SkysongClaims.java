package org.skysongdev.skysongClaims;

import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongClaims.apiimpl.ApiRegistration;
import org.skysongdev.skysongClaims.apiimpl.SkysongClaimsApiImpl;
import org.skysongdev.skysongClaims.config.ConfigException;
import org.skysongdev.skysongClaims.config.ConfigHolder;
import org.skysongdev.skysongClaims.config.ConfigLoader;
import org.skysongdev.skysongClaims.config.YamlFiles;

/**
 * The plugin class. It only builds the features and connects them; every rule lives in the
 * feature classes.
 */
public final class SkysongClaims extends JavaPlugin {
    private ApiRegistration api;

    @Override
    public void onEnable() {
        ConfigHolder config;
        try {
            YamlFiles files = new YamlFiles(getDataFolder().toPath(), path -> saveResource(path, false));
            config = new ConfigHolder(new ConfigLoader(files, getLogger()::warning)::load);
        } catch (ConfigException e) {
            getLogger().severe("SkysongClaims is disabled because its config is not valid: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Loaded " + config.current().types().all().size() + " claim types");

        // The API is offered last, once everything behind it is running.
        api = new ApiRegistration(getServer().getServicesManager(), new SkysongClaimsApiImpl());
        api.register(this);
    }

    @Override
    public void onDisable() {
        // Taken back first, so no plugin calls into features that are shutting down.
        if (api != null) {
            api.unregister();
        }
    }
}
