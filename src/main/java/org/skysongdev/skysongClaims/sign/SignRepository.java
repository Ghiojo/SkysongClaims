package org.skysongdev.skysongClaims.sign;

import java.util.List;

/** Where Claim signs are kept between restarts. Deleting a Claim deletes its signs. */
public interface SignRepository {
    List<ClaimSign> loadAll();

    /** Saves the sign, replacing the one at its location. */
    void save(ClaimSign sign);

    void delete(SignLocation location);
}
