package org.skysongdev.skysongClaims.claim;

/**
 * Told about every Claim change after it is made and saved, for example to update WorldGuard,
 * redraw signs or fire API events. Observers must not change Claims themselves.
 */
@FunctionalInterface
public interface ClaimObserver {
    void claimChanged(ClaimChange change);
}
