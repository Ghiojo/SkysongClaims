package org.skysongdev.skysongClaims.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skysongdev.skysongClaims.api.LimitScope;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypesParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The default files in the jar must load with no warnings, the way a new server first sees them. */
class BundledConfigTest {
    @TempDir
    Path dataFolder;

    private void copyFromJar(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " is bundled");
            Files.copy(in, dataFolder.resolve(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void defaultFilesLoadCleanly() {
        List<String> warnings = new ArrayList<>();
        ConfigLoader loader = new ConfigLoader(new YamlFiles(dataFolder, this::copyFromJar), TestMessages::bundled, warnings::add);

        LoadedConfig config = loader.load();

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(LimitScope.CHARACTER, config.settings().defaultLimitScope());
        ClaimType hovel = config.types().find("hovel").orElseThrow();
        assertTrue(hovel.freeClaim());
        assertEquals(2, hovel.maxOwners());
        assertEquals(ClaimTypesParser.DEFAULT_OPEN_USE, hovel.openUse());
        assertEquals(3, config.types().find("other").orElseThrow().limit());
        assertTrue(Files.exists(dataFolder.resolve("types.yml")), "missing files are created");
        assertTrue(config.messages().has("claims.created"));
    }
}
