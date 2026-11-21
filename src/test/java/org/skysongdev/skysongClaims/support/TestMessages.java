package org.skysongdev.skysongClaims.support;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.skysongdev.skysongClaims.config.ConfigSection;
import org.skysongdev.skysongClaims.config.YamlFiles;
import org.skysongdev.skysongClaims.messaging.MessageCatalog;
import org.skysongdev.skysongClaims.messaging.Messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** The bundled messages.yml, for tests that send messages. */
public final class TestMessages {
    private TestMessages() {
    }

    /** @return a file bundled in the plugin jar */
    public static ConfigSection bundled(String path) {
        InputStream in = Objects.requireNonNull(TestMessages.class.getClassLoader().getResourceAsStream(path), path);
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlFiles.parse(path, reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MessageCatalog catalog() {
        return MessageCatalog.of(bundled(MessageCatalog.FILE), ConfigSection.root("empty", null), warning -> {
        });
    }

    public static Messenger messenger() {
        MessageCatalog catalog = catalog();
        return new Messenger(MiniMessage.miniMessage(), () -> catalog);
    }
}
