package org.skysongdev.skysongClaims.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point of the SkysongClaims API, the supported way for other plugins to read and
 * change Claims. Classes outside the {@code org.skysongdev.skysongClaims.api} package are
 * internal and may change without notice.
 *
 * <p>SkysongClaims registers this API with Bukkit's {@link ServicesManager} at the end of its
 * {@code onEnable} and removes it in {@code onDisable}. Plugins that use it add
 * {@code depend: [SkysongClaims]} (or {@code softdepend}) to their plugin.yml, so they enable
 * after SkysongClaims, and get it with {@link #get()}:
 *
 * <pre>{@code
 * SkysongClaimsApi claims = SkysongClaimsApi.get();
 * }</pre>
 *
 * <p>Methods that read or change Claims must be called on the server's main thread and throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>SkysongClaims implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface SkysongClaimsApi {

    /**
     * The API registered by SkysongClaims.
     *
     * @return the API
     * @throws IllegalStateException when SkysongClaims is not installed or not enabled yet
     */
    static @NotNull SkysongClaimsApi get() {
        return get(Bukkit.getServicesManager());
    }

    /**
     * The API registered in a services manager. {@link #get()} uses the server's.
     *
     * @param services where SkysongClaims registered its API
     * @return the API
     * @throws IllegalStateException when no API is registered there
     */
    @ApiStatus.Internal
    static @NotNull SkysongClaimsApi get(@NotNull ServicesManager services) {
        RegisteredServiceProvider<SkysongClaimsApi> registration = services.getRegistration(SkysongClaimsApi.class);
        if (registration == null) {
            throw new IllegalStateException("The SkysongClaims API is not available: SkysongClaims is not enabled."
                    + " Add 'depend: [SkysongClaims]' to your plugin.yml and check that SkysongClaims is installed"
                    + " and started without errors.");
        }
        return registration.getProvider();
    }

    /**
     * The version of the API that the installed SkysongClaims implements, as
     * {@code major.minor.patch}. A plugin can compare it with the version it was built
     * against and log a clear message instead of failing with {@link NoSuchMethodError}.
     * Adding methods raises the minor version; removing or changing one raises the major.
     *
     * @return the API version, for example {@code "1.0.0"}
     */
    @NotNull String apiVersion();
}
