package org.skysongdev.skysongClaims.limit;

import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimOwner;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.LimitScope;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;

/**
 * How many Claims of a type someone may own: the type's base limit plus every live rule. A
 * shared Claim counts once for each of its owners. Someone over their limit keeps what they
 * own but can't take more.
 */
public final class Limits {
    private final ClaimState claims;
    private final LimitRules rules;

    public Limits(ClaimState claims, LimitRules rules) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /** @return who the type's limit counts for when this character takes a Claim of it */
    public static LimitSubject subject(CharacterId character, ClaimType type) {
        return type.limitScope() == LimitScope.ACCOUNT
                ? LimitSubject.ofAccount(character.player())
                : LimitSubject.ofCharacter(character);
    }

    /** @return the limit, never below 0 */
    public int limit(LimitSubject subject, ClaimType type) {
        return Math.max(0, type.limit() + rules.bonus(subject, type));
    }

    /** @return how many Claims of the type the subject owns, each Claim once */
    public int count(LimitSubject subject, ClaimType type) {
        return (int) claims.all().stream()
                .filter(claim -> claim.typeId().equals(type.id()))
                .filter(claim -> ownedBy(claim, subject))
                .count();
    }

    /** @return how many more the subject may take, never below 0 */
    public int remaining(LimitSubject subject, ClaimType type) {
        return Math.max(0, limit(subject, type) - count(subject, type));
    }

    private static boolean ownedBy(Claim claim, LimitSubject subject) {
        return claim.owners().stream().map(ClaimOwner::character).anyMatch(subject::counts);
    }
}
