package org.skysongdev.skysongClaims.api.event;

/** Why a Claim changed, carried by every SkysongClaims event. */
public enum ChangeCause {
    /** A player or staff member ran a command, or used a Claim sign's menu (except leaving: {@link #LEAVE}). */
    COMMAND,
    /** An owner left the Claim, by command or from a Claim sign's menu. */
    LEAVE,
    /** A player took a slot of a Free Claim. */
    FREE_CLAIM,
    /** Another plugin changed it through the API. */
    API,
    /** SkysongClaims fixed it on its own, for example releasing a deleted profile. */
    RECONCILE
}
