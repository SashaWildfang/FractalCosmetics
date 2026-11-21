/*
 * FractalCosmetics - built by Sasha Wildfang for Fractal Network.
 */
package com.sasha.cosmetics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The built plugin.yml is valid YAML (Paper refuses to load the plugin otherwise). */
class PluginYmlTest {

    @Test
    void pluginYmlParses() throws Exception {
        try (InputStream in = PluginYmlTest.class.getResourceAsStream("/plugin.yml")) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(text); // throws on invalid YAML
            assertEquals("FractalCosmetics", yaml.getString("name"));
            assertEquals("com.sasha.cosmetics.FractalCosmetics", yaml.getString("main"));
            assertEquals(List.of("FractalLobby"), yaml.getStringList("depend"));
            assertTrue(yaml.getString("description", "").contains("Fractal Network's lobby"), yaml.getString("description"));
            assertTrue(!yaml.getString("version", "").contains("${"), "version is filled in");
        }
    }
}
