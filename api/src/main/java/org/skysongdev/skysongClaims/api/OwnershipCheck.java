package org.skysongdev.skysongClaims.api;

/** Whether a character may become an owner of a Claim, and if not, why. */
public enum OwnershipCheck {
    /** It may, or it just did. */
    OK,
    /** It already owns as many Claims of the type as its limit allows. */
    TYPE_LIMIT,
    /** Every owner slot of the Claim is taken. */
    FULL,
    /** It already owns the Claim. */
    ALREADY_OWNER,
    /** SkysongStats has no such character. */
    UNKNOWN_CHARACTER
}
