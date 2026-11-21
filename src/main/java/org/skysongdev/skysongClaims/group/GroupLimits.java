package org.skysongdev.skysongClaims.group;

/**
 * How many build groups one Claim may have, and characters one group may hold; 0 means no limit.
 *
 * @param groupsPerClaim  {@code max-groups-per-claim} in config.yml
 * @param membersPerGroup {@code max-members-per-group} in config.yml
 */
public record GroupLimits(int groupsPerClaim, int membersPerGroup) {
}
