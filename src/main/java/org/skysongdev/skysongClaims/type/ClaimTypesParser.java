package org.skysongdev.skysongClaims.type;

import org.skysongdev.skysongClaims.api.LimitScope;
import org.skysongdev.skysongClaims.config.ConfigException;
import org.skysongdev.skysongClaims.config.ConfigSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Reads {@code types.yml}:
 * <pre>{@code
 * types:
 *   hovel:
 *     display-name: "<gold>Hovel"
 *     limit: 1
 *     limit-per: character          # or account; config.yml's limit-per when left out
 *     free-claim: { default-max-owners: 2 }
 *     open-use: [use, chest-access]  # the standard open-use flags when left out
 *     extra: {}                      # read by other plugins
 *   other:
 *     display-name: "Other"
 *     limit: 3
 * }</pre>
 * A type with a {@code free-claim} section makes new Claims that players take themselves, one
 * slot each. Claims of other types are bought or given by staff and have no cap on owners.
 */
public final class ClaimTypesParser {
    /** File name in the data folder and in the jar. */
    public static final String FILE = "types.yml";

    /**
     * The WorldGuard flags opened to everyone when a type lists none: doors, chests, beds,
     * workstations, vehicles and item frames. Animals keep WorldGuard's default.
     */
    public static final List<String> DEFAULT_OPEN_USE = List.of(
            "use", "chest-access", "interact", "sleep", "use-anvil", "use-dripleaf", "respawn-anchors",
            "ride", "vehicle-place", "vehicle-destroy", "item-frame-rotation");

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern FLAG_NAME = Pattern.compile("[a-z0-9_-]+");

    private final LimitScope defaultScope;
    private final Consumer<String> warnings;

    /**
     * @param defaultScope the scope of types that don't set {@code limit-per}, from config.yml
     * @param warnings     receives one message per problem that was skipped
     */
    public ClaimTypesParser(LimitScope defaultScope, Consumer<String> warnings) {
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** @throws ConfigException when a type is unusable, for example it has a bad id or no name */
    public ClaimTypeRegistry parse(ConfigSection root) {
        ConfigSection types = root.section("types");
        if (types.keys().isEmpty()) {
            warnings.accept(types.path() + " defines no claim types, so no Claim can be created");
        }
        List<ClaimType> parsed = new ArrayList<>();
        for (String id : types.keys()) {
            if (!ID.matcher(id).matches()) {
                throw types.error(id, "is not a valid id: use up to 32 lower case letters, digits, '_' and '-'");
            }
            parsed.add(parseType(id, types.section(id)));
        }
        return new ClaimTypeRegistry(parsed);
    }

    private ClaimType parseType(String id, ConfigSection section) {
        String displayName = section.requireString("display-name");
        int limit = nonNegative(section, "limit", requiredInteger(section, "limit"));
        LimitScope scope = scope(section);
        boolean free = section.contains("free-claim");
        OptionalInt slots = free
                ? OptionalInt.of(atLeastOne(section.section("free-claim"), "default-max-owners",
                        section.section("free-claim").integer("default-max-owners", 1)))
                : OptionalInt.empty();
        if (section.contains("max-owners")) {
            warnings.accept(section.path() + ".max-owners is ignored: bought Claims have no cap on owners, "
                    + "and a Free Claim type sets its slots with free-claim.default-max-owners");
        }
        return new ClaimType(id, displayName, limit, scope, slots, openUse(section),
                section.plainSection("extra"));
    }

    private LimitScope scope(ConfigSection section) {
        String raw = section.string("limit-per", null);
        if (raw == null) {
            return defaultScope;
        }
        return LimitScope.fromConfig(raw)
                .orElseThrow(() -> section.error("limit-per", "must be 'character' or 'account', not '" + raw + "'"));
    }

    private List<String> openUse(ConfigSection section) {
        if (!section.contains("open-use")) {
            return DEFAULT_OPEN_USE;
        }
        Set<String> flags = new LinkedHashSet<>();
        for (String raw : section.strings("open-use")) {
            String flag = raw.trim().toLowerCase(Locale.ROOT);
            if (!FLAG_NAME.matcher(flag).matches()) {
                warnings.accept(section.path() + ".open-use: '" + raw + "' is not a flag name and was skipped");
                continue;
            }
            if (flag.equals("build")) {
                warnings.accept(section.path() + ".open-use: 'build' can't be opened to everyone and was skipped");
                continue;
            }
            flags.add(flag);
        }
        return List.copyOf(flags);
    }

    private static int requiredInteger(ConfigSection section, String key) {
        if (!section.contains(key)) {
            throw section.error(key, "is required");
        }
        return section.integer(key, 0);
    }

    private static int nonNegative(ConfigSection section, String key, int value) {
        if (value < 0) {
            throw section.error(key, "must be 0 or more");
        }
        return value;
    }

    private static int atLeastOne(ConfigSection section, String key, int value) {
        if (value < 1) {
            throw section.error(key, "must be 1 or more");
        }
        return value;
    }
}
