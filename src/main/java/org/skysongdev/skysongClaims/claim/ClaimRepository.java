package org.skysongdev.skysongClaims.claim;

import java.util.List;

/**
 * Where Claims are kept between restarts. Every Claim is loaded once at start-up; after that
 * the plugin works from memory and saves each Claim whole when it changes.
 */
public interface ClaimRepository {
    /** @return every saved Claim */
    List<Claim> loadAll();

    /** Saves a Claim with its regions, owners and groups, replacing what was saved before. */
    void save(Claim claim);

    /** Deletes a Claim and everything that belongs to it. Does nothing when it isn't saved. */
    void delete(String claimId);
}
