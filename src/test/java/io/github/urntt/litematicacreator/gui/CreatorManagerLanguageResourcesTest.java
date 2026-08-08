package io.github.urntt.litematicacreator.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class CreatorManagerLanguageResourcesTest
{
    @Test
    void chineseAndEnglishResourcesExposeTheSameKeys() throws IOException
    {
        JsonObject chinese = load("zh_cn");
        JsonObject english = load("en_us");

        assertEquals(english.keySet(), chinese.keySet());
        assertFalse(chinese.isEmpty());
    }

    @Test
    void managerUsesExplicitActionsAndLocalizedErrors() throws IOException
    {
        JsonObject chinese = load("zh_cn");
        JsonObject english = load("en_us");

        assertEquals("确认重命名", value(chinese, "litematica-creator.gui.manager.rename"));
        assertEquals("设为 Focus", value(chinese, "litematica-creator.gui.manager.set_focus"));
        assertEquals("设为 Litematica Selected", value(chinese, "litematica-creator.gui.manager.set_selected"));
        assertEquals("Confirm rename", value(english, "litematica-creator.gui.manager.rename"));
        assertFalse(value(chinese, "litematica-creator.gui.manager.error.target_bound").startsWith("Target file"));
    }

    @Test
    void everyManagerControlGroupHasBilingualHoverText() throws IOException
    {
        JsonObject chinese = load("zh_cn");
        JsonObject english = load("en_us");
        Set<String> required = Set.of(
                "open", "search", "done", "clear_focus", "clear_selected",
                "tab.overview", "tab.placement", "tab.save_export",
                "apply", "thumbnail.capture", "thumbnail.clear", "reload", "unload",
                "rename", "enabled", "render", "set_focus", "set_selected", "native_config", "remove_placement",
                "export_mode.raw", "export_mode.sparse_compact", "export_mode.enclosing_projection",
                "export_mode.enclosing_with_world", "sampling", "preview", "save", "save_bind", "export_copy"
        );

        for (String suffix : required)
        {
            String key = "litematica-creator.gui.manager.hover." + suffix;
            assertTrue(chinese.has(key), key);
            assertTrue(english.has(key), key);
            assertFalse(value(chinese, key).isBlank(), key);
            assertFalse(value(english, key).isBlank(), key);
        }
    }

    private static JsonObject load(String locale) throws IOException
    {
        Path path = Path.of("src", "main", "resources", "assets", "litematica-creator", "lang", locale + ".json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static String value(JsonObject object, String key)
    {
        return object.get(key).getAsString();
    }
}
