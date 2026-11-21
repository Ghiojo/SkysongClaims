package org.skysongdev.skysongClaims.api;

import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A Claim as it was when it was read: a group of WorldGuard regions owned together. It doesn't
 * change when the Claim does; read it again for the latest.
 *
 * @param id          the name staff gave it, lower case
 * @param displayName the name shown to players, in MiniMessage
 * @param typeId      the id of its type in {@code types.yml}
 * @param slots       how many owners a Free Claim has room for, each taking one slot themselves;
 *                    empty for a bought Claim, which has no cap on owners
 * @param regions     its WorldGuard regions
 * @param owners      its owners, in the order they joined
 * @param forSaleBy   the plugin that put it on sale, empty when it isn't for sale
 */
public record ClaimInfo(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull String typeId,
        @NotNull OptionalInt slots,
        @NotNull List<ClaimRegion> regions,
        @NotNull List<ClaimOwner> owners,
        @NotNull Optional<String> forSaleBy) {

    public ClaimInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(slots, "slots");
        regions = List.copyOf(regions);
        owners = List.copyOf(owners);
        Objects.requireNonNull(forSaleBy, "forSaleBy");
    }

    public boolean isOwnedBy(@NotNull CharacterId character) {
        return owners.stream().anyMatch(owner -> owner.character().equals(character));
    }

    public boolean isOwned() {
        return !owners.isEmpty();
    }

    public boolean isForSale() {
        return forSaleBy.isPresent();
    }

    /** @return whether players take it themselves, one slot each */
    public boolean free() {
        return slots.isPresent();
    }

    /** @return how many slots of a Free Claim are open; empty for a bought Claim */
    public @NotNull OptionalInt openSlots() {
        return slots.isPresent() ? OptionalInt.of(Math.max(0, slots.getAsInt() - owners.size())) : OptionalInt.empty();
    }

    /** @return whether one more owner fits: always for a bought Claim, while a slot is open for a Free Claim */
    public boolean hasRoom() {
        return slots.isEmpty() || owners.size() < slots.getAsInt();
    }
}
