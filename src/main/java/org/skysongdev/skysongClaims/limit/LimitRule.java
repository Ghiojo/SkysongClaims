package org.skysongdev.skysongClaims.limit;

import org.skysongdev.skysongClaims.type.ClaimType;

/**
 * A live rule from another plugin that raises (or lowers) a limit, worked out every time a
 * limit is checked, for example "+1 hovel while they have the Noble rank". SkysongClaims saves
 * nothing for it, so a plugin that wants a lasting reward keeps it in its own data.
 */
@FunctionalInterface
public interface LimitRule {
    /** @return how many Claims of the type to add to the subject's limit; 0 for none, negative to lower it */
    int bonus(LimitSubject subject, ClaimType type);
}
