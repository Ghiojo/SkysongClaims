package org.skysongdev.skysongClaims.claim;

import java.util.Locale;
import java.util.Optional;

/**
 * What the members of an owner's group may do in the Claim. Using doors, chests and the like
 * is open to everyone, so it is never a group permission.
 */
public enum GroupPermission {
    /** Place and break blocks in every region of the Claim, mannequins included. */
    BUILD;

    /** @return the permission with that name, ignoring case */
    public static Optional<GroupPermission> fromName(String name) {
        for (GroupPermission permission : values()) {
            if (permission.configName().equalsIgnoreCase(name.trim())) {
                return Optional.of(permission);
            }
        }
        return Optional.empty();
    }

    /** @return how commands and the database write it */
    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
