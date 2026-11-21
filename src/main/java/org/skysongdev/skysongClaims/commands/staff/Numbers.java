package org.skysongdev.skysongClaims.commands.staff;

import org.skysongdev.skysongClaims.messaging.RefusedException;

/** Reads whole numbers that staff type. */
final class Numbers {
    private Numbers() {
    }

    /** @throws RefusedException {@code command.not-a-number} when it isn't a whole number */
    static int parse(String typed) {
        try {
            return Integer.parseInt(typed);
        } catch (NumberFormatException e) {
            throw RefusedException.of("command.not-a-number", "input", typed);
        }
    }
}
