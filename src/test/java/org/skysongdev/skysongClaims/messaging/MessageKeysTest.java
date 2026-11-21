package org.skysongdev.skysongClaims.messaging;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.support.TestMessages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every message key the code sends must be in the bundled messages.yml, so no player sees "Missing message". */
class MessageKeysTest {
    /** A quoted key like {@code "claim.unknown"} passed to send(...), RefusedException.of(...) or render(...). */
    private static final Pattern KEY = Pattern.compile(
            "(?:send\\([^\"]*?|RefusedException\\.of\\(|render\\()\"([a-z0-9-]+(?:\\.[a-z0-9-]+)+)\"");
    /** A ternary choosing between two keys: {@code ? "a.b" : "a.c"}. */
    private static final Pattern CHOICE = Pattern.compile("[?:]\\s*\"([a-z0-9-]+(?:\\.[a-z0-9-]+)+)\"");

    @Test
    void everySentKeyExists() throws IOException {
        MessageCatalog catalog = TestMessages.catalog();
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (Pattern pattern : new Pattern[]{KEY, CHOICE}) {
                    Matcher matcher = pattern.matcher(source);
                    while (matcher.find()) {
                        if (!catalog.has(matcher.group(1))) {
                            missing.add(matcher.group(1) + " (" + file.getFileName() + ")");
                        }
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
    }
}
