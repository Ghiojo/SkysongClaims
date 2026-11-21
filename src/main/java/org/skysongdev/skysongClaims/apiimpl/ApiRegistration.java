package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.skysongdev.skysongClaims.api.SkysongClaimsApi;

import java.util.Objects;

/** Offers the API to other plugins through a services manager, and takes it back. */
public final class ApiRegistration {
    private final ServicesManager services;
    private final SkysongClaimsApi api;
    private boolean registered;

    public ApiRegistration(ServicesManager services, SkysongClaimsApi api) {
        this.services = Objects.requireNonNull(services, "services");
        this.api = Objects.requireNonNull(api, "api");
    }

    /** Registers the API for this plugin. Registering twice does nothing. */
    public void register(Plugin plugin) {
        if (registered) {
            return;
        }
        services.register(SkysongClaimsApi.class, api, plugin, ServicePriority.Normal);
        registered = true;
    }

    /** Removes the API. Safe to call when it was never registered. */
    public void unregister() {
        if (!registered) {
            return;
        }
        services.unregister(SkysongClaimsApi.class, api);
        registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }
}
