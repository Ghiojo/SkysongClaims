package org.skysongdev.skysongClaims.limit;

import org.skysongdev.skysongClaims.api.LimitRule;
import org.skysongdev.skysongClaims.api.LimitSubject;
import org.skysongdev.skysongClaims.type.ClaimType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/** The live limit rules other plugins registered, by plugin and rule id. */
public final class LimitRules {
    private record Key(String plugin, String id) {
    }

    private final Map<Key, LimitRule> rules = new LinkedHashMap<>();
    private final BiConsumer<String, RuntimeException> failures;

    /** @param failures told about a rule that threw; it then counts as 0 */
    public LimitRules(BiConsumer<String, RuntimeException> failures) {
        this.failures = Objects.requireNonNull(failures, "failures");
    }

    /** Adds a rule, or replaces the plugin's rule with that id. */
    public void register(String plugin, String id, LimitRule rule) {
        rules.put(new Key(plugin, id), Objects.requireNonNull(rule, "rule"));
    }

    public void unregister(String plugin, String id) {
        rules.remove(new Key(plugin, id));
    }

    /** Removes every rule of a plugin, for example when it disables. */
    public void unregisterAll(String plugin) {
        rules.keySet().removeIf(key -> key.plugin().equals(plugin));
    }

    /** @return the sum of every rule's bonus */
    public int bonus(LimitSubject subject, ClaimType type) {
        int total = 0;
        for (Map.Entry<Key, LimitRule> entry : rules.entrySet()) {
            try {
                total += entry.getValue().bonus(subject, type.info());
            } catch (RuntimeException e) {
                failures.accept("The limit rule " + entry.getKey().id() + " of " + entry.getKey().plugin() + " failed", e);
            }
        }
        return total;
    }
}
