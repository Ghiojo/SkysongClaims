package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.NotNull;

/**
 * A live rule that raises (or lowers) a limit, worked out every time SkysongClaims checks one,
 * for example "+1 hovel while they have the Noble rank". SkysongClaims saves nothing for it, so
 * a plugin that wants a lasting reward keeps it in its own data. Register it with
 * {@link Limits#registerRule}.
 *
 * <p>It is called on the main thread, often, so it must be quick and must not block.
 */
@FunctionalInterface
public interface LimitRule {
    /**
     * @param subject who the limit is for: a character, or a whole account when the type's limit is per account
     * @param type    the Claim type
     * @return how many Claims of the type to add to the subject's limit; 0 for none, negative to lower it
     */
    int bonus(@NotNull LimitSubject subject, @NotNull ClaimTypeInfo type);
}
