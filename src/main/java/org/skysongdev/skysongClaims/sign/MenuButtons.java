package org.skysongdev.skysongClaims.sign;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The buttons other plugins added to Claim sign menus, by plugin and id, in the order they were added. */
public final class MenuButtons {
    private record Key(String plugin, String id) {
    }

    private final Map<Key, MenuButton> buttons = new LinkedHashMap<>();

    /** Adds a button, or replaces the plugin's button with that id. */
    public void add(String plugin, String id, MenuButton button) {
        buttons.put(new Key(plugin, id), Objects.requireNonNull(button, "button"));
    }

    public void remove(String plugin, String id) {
        buttons.remove(new Key(plugin, id));
    }

    /** Removes every button of a plugin, for example when it disables. */
    public void removeAll(String plugin) {
        buttons.keySet().removeIf(key -> key.plugin().equals(plugin));
    }

    public List<MenuButton> all() {
        return List.copyOf(buttons.values());
    }
}
