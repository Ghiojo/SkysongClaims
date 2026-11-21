package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.LimitRule;
import org.skysongdev.skysongClaims.api.LimitSubject;
import org.skysongdev.skysongClaims.api.Limits;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;

/** {@link Limits} over the internal limits and rules. */
final class LimitsApi implements Limits {
    private final ApiLookup lookup;
    private final org.skysongdev.skysongClaims.limit.Limits limits;
    private final LimitRules rules;

    LimitsApi(ApiLookup lookup, org.skysongdev.skysongClaims.limit.Limits limits, LimitRules rules) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    @Override
    public @NotNull LimitSubject subject(@NotNull CharacterId character, @NotNull String typeId) {
        return org.skysongdev.skysongClaims.limit.Limits.subject(Objects.requireNonNull(character, "character"),
                lookup.type(typeId));
    }

    @Override
    public int limit(@NotNull LimitSubject subject, @NotNull String typeId) {
        return limits.limit(Objects.requireNonNull(subject, "subject"), lookup.type(typeId));
    }

    @Override
    public int count(@NotNull LimitSubject subject, @NotNull String typeId) {
        return limits.count(Objects.requireNonNull(subject, "subject"), lookup.type(typeId));
    }

    @Override
    public int remaining(@NotNull LimitSubject subject, @NotNull String typeId) {
        return limits.remaining(Objects.requireNonNull(subject, "subject"), lookup.type(typeId));
    }

    @Override
    public void registerRule(@NotNull Plugin plugin, @NotNull String id, @NotNull LimitRule rule) {
        lookup.checkThread();
        rules.register(plugin.getName(), Objects.requireNonNull(id, "id"), rule);
    }

    @Override
    public void unregisterRule(@NotNull Plugin plugin, @NotNull String id) {
        lookup.checkThread();
        rules.unregister(plugin.getName(), id);
    }
}
