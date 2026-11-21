package org.skysongdev.skysongClaims.sign;

import java.util.Objects;
import java.util.Optional;

/**
 * A sign that shows a Claim. SkysongClaims writes its first three lines; the fourth is the
 * price line another plugin (SkysongEconomy) sets, saved here so it is written once.
 *
 * @param claimId   the Claim it shows
 * @param location  where it stands
 * @param priceLine the fourth line as Adventure JSON, empty when blank
 */
public record ClaimSign(String claimId, SignLocation location, Optional<String> priceLine) {
    public ClaimSign {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(priceLine, "priceLine");
    }

    public ClaimSign withPriceLine(Optional<String> newPriceLine) {
        return new ClaimSign(claimId, location, newPriceLine);
    }
}
