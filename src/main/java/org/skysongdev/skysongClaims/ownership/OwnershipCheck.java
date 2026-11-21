package org.skysongdev.skysongClaims.ownership;

/** Whether a character may become an owner of a Claim, and if not, why. */
public enum OwnershipCheck {
    OK("ownership.ok"),
    /** They already own as many Claims of its type as their limit allows. */
    TYPE_LIMIT("ownership.type-limit"),
    /** Every owner slot is taken. */
    FULL("ownership.full"),
    ALREADY_OWNER("ownership.already-owner"),
    /** SkysongStats has no such character. */
    UNKNOWN_CHARACTER("ownership.unknown-character");

    private final String messageKey;

    OwnershipCheck(String messageKey) {
        this.messageKey = messageKey;
    }

    /** @return the {@code messages.yml} key that explains a refusal */
    public String messageKey() {
        return messageKey;
    }
}
