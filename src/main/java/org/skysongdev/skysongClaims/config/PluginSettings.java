package org.skysongdev.skysongClaims.config;

import org.skysongdev.skysongClaims.group.GroupLimits;
import org.skysongdev.skysongClaims.storage.DatabaseSettings;
import org.skysongdev.skysongClaims.type.LimitScope;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The plugin-wide settings of {@code config.yml}.
 *
 * @param database          where Claims are saved; read once at start-up
 * @param defaultLimitScope whether limits count per character or per account, for types that don't say
 * @param inviteExpiry      how long a co-owner invite waits for an answer
 * @param groupLimits       how many groups a Claim may have and members a group may have; 0 for no limit
 * @param ownerFlags        the WorldGuard flags owners may set with {@code /sclaim flag}, lower case
 */
public record PluginSettings(DatabaseSettings database, LimitScope defaultLimitScope, Duration inviteExpiry,
                             GroupLimits groupLimits, List<String> ownerFlags) {
    /** File name in the data folder and in the jar. */
    public static final String FILE = "config.yml";

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*([smh])");

    public PluginSettings {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(defaultLimitScope, "defaultLimitScope");
        Objects.requireNonNull(inviteExpiry, "inviteExpiry");
        Objects.requireNonNull(groupLimits, "groupLimits");
        ownerFlags = List.copyOf(ownerFlags);
    }

    /** @throws ConfigException when a setting has a value the plugin can't use */
    public static PluginSettings parse(ConfigSection root) {
        String raw = root.string("limit-per", LimitScope.CHARACTER.configName());
        LimitScope scope = LimitScope.fromConfig(raw)
                .orElseThrow(() -> root.error("limit-per", "must be 'character' or 'account', not '" + raw + "'"));
        return new PluginSettings(DatabaseSettings.parse(root.section("database")), scope,
                duration(root, "invite-expiry", "5m"),
                new GroupLimits(atLeastZero(root, "max-groups-per-claim", 5),
                        atLeastZero(root, "max-members-per-group", 0)),
                root.contains("owner-flags")
                        ? root.strings("owner-flags").stream().map(flag -> flag.trim().toLowerCase(Locale.ROOT)).toList()
                        : List.of("greeting", "farewell"));
    }

    private static int atLeastZero(ConfigSection root, String key, int fallback) {
        int value = root.integer(key, fallback);
        if (value < 0) {
            throw root.error(key, "must be 0 (no limit) or more");
        }
        return value;
    }

    /** Reads a duration such as {@code 30s}, {@code 5m} or {@code 1h}. */
    private static Duration duration(ConfigSection root, String key, String fallback) {
        String raw = root.string(key, fallback).trim().toLowerCase(Locale.ROOT);
        Matcher matcher = DURATION.matcher(raw);
        if (!matcher.matches() || Long.parseLong(matcher.group(1)) == 0) {
            throw root.error(key, "must be a time such as 30s, 5m or 1h, not '" + raw + "'");
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            default -> Duration.ofHours(amount);
        };
    }
}
