package org.skysongdev.skysongClaims.apiimpl;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimSigns;
import org.skysongdev.skysongClaims.api.ClaimTypes;
import org.skysongdev.skysongClaims.api.Claims;
import org.skysongdev.skysongClaims.api.Groups;
import org.skysongdev.skysongClaims.api.Limits;
import org.skysongdev.skysongClaims.api.Market;
import org.skysongdev.skysongClaims.api.Ownership;
import org.skysongdev.skysongClaims.api.SkysongClaimsApi;
import org.skysongdev.skysongClaims.claim.ClaimLocator;
import org.skysongdev.skysongClaims.claim.ClaimService;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.sign.MenuButtons;
import org.skysongdev.skysongClaims.sign.SignService;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;

import java.util.function.Supplier;

/** SkysongClaims' implementation of its public API: one part per feature, over that feature's service. */
public final class SkysongClaimsApiImpl implements SkysongClaimsApi {
    /** The API version this implementation provides. See {@link SkysongClaimsApi#apiVersion()}. */
    public static final String API_VERSION = "1.0.0";

    /** The internal features the API reads and changes. */
    public record Features(
            ClaimState state,
            Supplier<ClaimTypeRegistry> types,
            ClaimLocator locator,
            ClaimService claims,
            OwnershipService ownership,
            org.skysongdev.skysongClaims.limit.Limits limits,
            LimitRules limitRules,
            SignService signs,
            MenuButtons menuButtons) {
    }

    private final Claims claims;
    private final ClaimTypes types;
    private final Ownership ownership;
    private final Market market;
    private final Groups groups;
    private final Limits limits;
    private final ClaimSigns signs;

    public SkysongClaimsApiImpl(Features features, MainThread mainThread) {
        ApiLookup lookup = new ApiLookup(mainThread, features.state(), features.types());
        this.claims = new ClaimsApi(lookup, features.locator());
        this.types = new ClaimTypesApi(lookup);
        this.ownership = new OwnershipApi(lookup, features.ownership());
        this.market = new MarketApi(lookup, features.claims());
        this.groups = new GroupsApi(lookup);
        this.limits = new LimitsApi(lookup, features.limits(), features.limitRules());
        this.signs = new ClaimSignsApi(lookup, features.signs(), features.menuButtons());
    }

    @Override
    public @NotNull String apiVersion() {
        return API_VERSION;
    }

    @Override
    public @NotNull Claims claims() {
        return claims;
    }

    @Override
    public @NotNull ClaimTypes types() {
        return types;
    }

    @Override
    public @NotNull Ownership ownership() {
        return ownership;
    }

    @Override
    public @NotNull Market market() {
        return market;
    }

    @Override
    public @NotNull Groups groups() {
        return groups;
    }

    @Override
    public @NotNull Limits limits() {
        return limits;
    }

    @Override
    public @NotNull ClaimSigns signs() {
        return signs;
    }
}
