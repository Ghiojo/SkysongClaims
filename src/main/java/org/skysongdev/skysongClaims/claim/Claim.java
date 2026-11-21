package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * A group of WorldGuard regions owned together. The Claim is the unit of ownership, typing,
 * limits and groups; its regions only carry the shape. Immutable: every change makes a new
 * Claim through a {@code with...} method.
 *
 * @param id          the name staff type, lower case, up to 32 characters
 * @param displayName the name shown to players, in MiniMessage
 * @param typeId      the id of its type in {@code types.yml}
 * @param slots       how many owners a Free Claim has room for, each taking one slot themselves;
 *                    empty for a bought Claim, which has no cap on owners
 * @param regions     its WorldGuard regions, at least one, in the order they were added
 * @param owners      its owners, in the order they joined
 * @param groups      its owners' groups, in the order they were made
 * @param saleSetBy   the plugin that put it on sale, empty when it isn't for sale
 * @param flags       the WorldGuard flags its owners set, by flag name, as they typed the value
 */
public record Claim(
        String id,
        String displayName,
        String typeId,
        OptionalInt slots,
        List<ClaimRegion> regions,
        List<ClaimOwner> owners,
        List<ClaimGroup> groups,
        Optional<String> saleSetBy,
        Map<String, String> flags) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    public Claim {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(saleSetBy, "saleSetBy");
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid claim id: " + id);
        }
        if (slots.isPresent() && slots.getAsInt() < 1) {
            throw new IllegalArgumentException("A Free Claim needs at least one slot: " + slots.getAsInt());
        }
        regions = List.copyOf(new LinkedHashSet<>(regions));
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A claim needs at least one region: " + id);
        }
        owners = List.copyOf(owners);
        groups = List.copyOf(groups);
        flags = Collections.unmodifiableMap(new TreeMap<>(flags));
    }

    /** @return a new, unowned Claim with no groups that isn't for sale; a Free Claim when it has slots */
    public static Claim create(String id, String typeId, OptionalInt slots, List<ClaimRegion> regions) {
        return new Claim(id, id, typeId, slots, regions, List.of(), List.of(), Optional.empty(), Map.of());
    }

    /** @return whether players take it themselves, one slot each */
    public boolean free() {
        return slots.isPresent();
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

    /** @return how many slots of a Free Claim are open, never negative; empty for a bought Claim */
    public OptionalInt openSlots() {
        return slots.isPresent() ? OptionalInt.of(Math.max(0, slots.getAsInt() - owners.size())) : OptionalInt.empty();
    }

    /** @return whether one more owner fits: always for a bought Claim, while a slot is open for a Free Claim */
    public boolean hasRoom() {
        return slots.isEmpty() || owners.size() < slots.getAsInt();
    }

    /** @return what the API shows of it: everything but its groups and flags */
    public ClaimInfo info() {
        return new ClaimInfo(id, displayName, typeId, slots, regions, owners, saleSetBy);
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
        return new Claim(id, newDisplayName, typeId, slots, regions, owners, groups, saleSetBy, flags);
    }

    public Claim withType(String newTypeId) {
        return new Claim(id, displayName, newTypeId, slots, regions, owners, groups, saleSetBy, flags);
    }

    /** @param newSlots the slots of a Free Claim, or empty to make it a bought Claim */
    public Claim withSlots(OptionalInt newSlots) {
        return new Claim(id, displayName, typeId, newSlots, regions, owners, groups, saleSetBy, flags);
    }

    public Claim withRegions(List<ClaimRegion> newRegions) {
        return new Claim(id, displayName, typeId, slots, newRegions, owners, groups, saleSetBy, flags);
    }

    public Claim withOwners(List<ClaimOwner> newOwners) {
        return new Claim(id, displayName, typeId, slots, regions, newOwners, groups, saleSetBy, flags);
    }

    public Claim withGroups(List<ClaimGroup> newGroups) {
        return new Claim(id, displayName, typeId, slots, regions, owners, newGroups, saleSetBy, flags);
    }

    public Claim withFlags(Map<String, String> newFlags) {
        return new Claim(id, displayName, typeId, slots, regions, owners, groups, saleSetBy, newFlags);
    }

    public Claim withSale(Optional<String> newSaleSetBy) {
        return new Claim(id, displayName, typeId, slots, regions, owners, groups, newSaleSetBy, flags);
    }
}
