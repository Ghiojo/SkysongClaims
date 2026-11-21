package org.skysongdev.skysongClaims;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginDescriptionTest {

    private static YamlConfiguration pluginYml() throws Exception {
        try (InputStream in = PluginDescriptionTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml is on the classpath");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void mainIsThePluginClass() throws Exception {
        assertEquals(SkysongClaims.class.getName(), pluginYml().getString("main"));
    }

    @Test
    void hardDependsOnWorldGuardAndSkysongStats() throws Exception {
        assertEquals(java.util.List.of("WorldGuard", "SkysongStats"), pluginYml().getStringList("depend"));
    }
}
