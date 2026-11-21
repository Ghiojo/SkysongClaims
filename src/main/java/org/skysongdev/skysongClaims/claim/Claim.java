package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongstats.api.CharacterId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A group of WorldGuard regions owned together. The Claim is the unit of ownership, typing,
 * limits and groups; its regions only carry the shape. Immutable: every change makes a new
 * Claim through a {@code with...} method.
 *
 * @param id          the name staff type, lower case, up to 32 characters
 * @param displayName the name shown to players, in MiniMessage
 * @param typeId      the id of its type in {@code types.yml}
 * @param free        whether players take it themselves, one slot each
 * @param maxOwners   how many owners it has room for
 * @param regions     its WorldGuard regions, at least one, in the order they were added
 * @param owners      its owners, in the order they joined
 * @param groups      its owners' groups, in the order they were made
 * @param saleSetBy   the plugin that put it on sale, empty when it isn't for sale
 */
public record Claim(
        String id,
        String displayName,
        String typeId,
        boolean free,
        int maxOwners,
        List<ClaimRegion> regions,
        List<ClaimOwner> owners,
        List<ClaimGroup> groups,
        Optional<String> saleSetBy) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    public Claim {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(saleSetBy, "saleSetBy");
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid claim id: " + id);
        }
        if (maxOwners < 1) {
            throw new IllegalArgumentException("maxOwners must be at least 1: " + maxOwners);
        }
        regions = List.copyOf(new LinkedHashSet<>(regions));
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A claim needs at least one region: " + id);
        }
        owners = List.copyOf(owners);
        groups = List.copyOf(groups);
    }

    /** @return a new, unowned Claim with no groups that isn't for sale */
    public static Claim create(String id, String typeId, boolean free, int maxOwners, List<ClaimRegion> regions) {
        return new Claim(id, id, typeId, free, maxOwners, regions, List.of(), List.of(), Optional.empty());
    }

    /** @return whether {@code id} can name a Claim: 1 to 32 lower case letters, digits, '_' or '-' */
    public static boolean isValidId(String id) {
        return ID.matcher(id).matches();
    }

    public boolean isOwnedBy(CharacterId character) {
        return owners.stream().anyMatch(owner -> owner.character().equals(character));
    }

    public boolean isOwned() {
        return !owners.isEmpty();
    }

    public boolean isForSale() {
        return saleSetBy.isPresent();
    }

    /** @return how many more owners it has room for, never negative */
    public int openSlots() {
        return Math.max(0, maxOwners - owners.size());
    }

    public Optional<ClaimGroup> group(String groupId) {
        return groups.stream().filter(group -> group.id().equals(groupId)).findFirst();
    }

    /**
     * @return the Claim without the character as an owner or group member. When it was the last
     *         owner the Claim is unowned again, so its groups are cleared.
     */
    public Claim withoutCharacter(CharacterId character) {
        List<ClaimOwner> remaining = owners.stream().filter(owner -> !owner.character().equals(character)).toList();
        if (remaining.isEmpty()) {
            return withOwners(remaining).withGroups(List.of());
        }
        List<ClaimGroup> cleaned = groups.stream().map(group -> group.withoutMember(character)).toList();
        return withOwners(remaining).withGroups(cleaned);
    }

    /** @return whether the character owns the Claim or is in one of its groups */
    public boolean involves(CharacterId character) {
        return isOwnedBy(character) || groups.stream().anyMatch(group -> group.members().contains(character));
    }

    public Claim withDisplayName(String newDisplayName) {
        return new Claim(id, newDisplayName, typeId, free, maxOwners, regions, owners, groups, saleSetBy);
    }

    public Claim withType(String newTypeId) {
        return new Claim(id, displayName, newTypeId, free, maxOwners, regions, owners, groups, saleSetBy);
    }

    public Claim withFree(boolean newFree, int newMaxOwners) {
        return new Claim(id, displayName, typeId, newFree, newMaxOwners, regions, owners, groups, saleSetBy);
    }

    public Claim withRegions(List<ClaimRegion> newRegions) {
        return new Claim(id, displayName, typeId, free, maxOwners, newRegions, owners, groups, saleSetBy);
    }

    public Claim withOwners(List<ClaimOwner> newOwners) {
        return new Claim(id, displayName, typeId, free, maxOwners, regions, newOwners, groups, saleSetBy);
    }

    public Claim withGroups(List<ClaimGroup> newGroups) {
        return new Claim(id, displayName, typeId, free, maxOwners, regions, owners, newGroups, saleSetBy);
    }

    public Claim withSale(Optional<String> newSaleSetBy) {
        return new Claim(id, displayName, typeId, free, maxOwners, regions, owners, groups, newSaleSetBy);
    }
}
