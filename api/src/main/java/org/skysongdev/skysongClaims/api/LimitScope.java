package org.skysongdev.skysongClaims.api;

import java.util.Locale;
import java.util.Optional;

/** Who a type's ownership limit counts for. */
public enum LimitScope {
    /** Each SkysongStats character has its own count, so a player's other characters don't use it up. */
    CHARACTER,
    /** One count for the whole player account, shared by all of its characters. */
    ACCOUNT;

    /** @return the scope with that config name ({@code character} or {@code account}), ignoring case */
    public static Optional<LimitScope> fromConfig(String name) {
        for (LimitScope scope : values()) {
            if (scope.configName().equalsIgnoreCase(name.trim())) {
                return Optional.of(scope);
            }
        }
        return Optional.empty();
    }

    /** @return how the config writes it */
    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
