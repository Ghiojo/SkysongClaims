package org.skysongdev.skysongClaims.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.SkysongClaimsApi;

/** SkysongClaims' implementation of its public API. Each feature adds its part here. */
public final class SkysongClaimsApiImpl implements SkysongClaimsApi {
    /** The API version this implementation provides. See {@link SkysongClaimsApi#apiVersion()}. */
    public static final String API_VERSION = "1.0.0";

    @Override
    public @NotNull String apiVersion() {
        return API_VERSION;
    }
}
