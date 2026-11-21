package org.skysongdev.skysongClaims.type;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Every Claim type from one load of {@code types.yml}, by id, in file order. Immutable. */
public final class ClaimTypeRegistry {
    private final Map<String, ClaimType> byId;
    private final List<ClaimType> ordered;

    /** @throws IllegalArgumentException when two types share an id */
    public ClaimTypeRegistry(Collection<ClaimType> types) {
        Map<String, ClaimType> ids = new LinkedHashMap<>();
        for (ClaimType type : types) {
            if (ids.putIfAbsent(type.id(), type) != null) {
                throw new IllegalArgumentException("Duplicate claim type " + type.id());
            }
        }
        this.byId = Map.copyOf(ids);
        this.ordered = List.copyOf(ids.values());
    }

    /** @return a registry with no types */
    public static ClaimTypeRegistry empty() {
        return new ClaimTypeRegistry(List.of());
    }

    /** @return the type with that id */
    public Optional<ClaimType> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** @return every type, in the order of the config file */
    public List<ClaimType> all() {
        return ordered;
    }
}
