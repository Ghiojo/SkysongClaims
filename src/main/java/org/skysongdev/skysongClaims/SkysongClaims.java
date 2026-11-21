package org.skysongdev.skysongClaims;

import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongClaims.apiimpl.ApiRegistration;
import org.skysongdev.skysongClaims.apiimpl.SkysongClaimsApiImpl;

/**
 * The plugin class. It only builds the features and connects them; every rule lives in the
 * feature classes.
 */
public final class SkysongClaims extends JavaPlugin {
    private ApiRegistration api;

    @Override
    public void onEnable() {
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
