package org.skysongdev.skysongClaims.claim;

/** How a character became an owner of a Claim. */
public enum OwnerCause {
    /** Staff added them with a command. */
    STAFF,
    /** They took a slot of a Free Claim themselves. */
    FREE_CLAIM,
    /** They accepted an owner's invite. */
    INVITE,
    /** Another plugin added them, for example SkysongEconomy after a sale. */
    API
}
