package org.skysongdev.skysongClaims.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An action was refused because of what was asked, for example taking a slot in a full Claim
 * or naming a region that doesn't exist. It carries the {@code messages.yml} key that explains
 * why and the values for that message's placeholders, which are always shown as plain text.
 */
public class RefusedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final transient Map<String, String> placeholders;

    /**
     * @param messageKey   the key in {@code messages.yml}
     * @param placeholders placeholder names, without brackets, and their values
     */
    public RefusedException(String messageKey, Map<String, String> placeholders) {
        super(messageKey + " " + placeholders);
        this.messageKey = messageKey;
        this.placeholders = Map.copyOf(placeholders);
    }

    /**
     * @param messageKey   the key in {@code messages.yml}
     * @param namesAndValues placeholder names and values, alternating: {@code "claim", "hovel_1"}
     */
    public static RefusedException of(String messageKey, String... namesAndValues) {
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders come in name/value pairs");
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            placeholders.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return new RefusedException(messageKey, placeholders);
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, String> placeholders() {
        return placeholders;
    }
}
