package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.sign.MenuButtons;

import java.util.Objects;

/** Drops a plugin's limit rules and menu buttons when it disables, so none calls into a plugin that is gone. */
public final class PluginCleanup implements Listener {
    private final LimitRules rules;
    private final MenuButtons buttons;

    public PluginCleanup(LimitRules rules, MenuButtons buttons) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.buttons = Objects.requireNonNull(buttons, "buttons");
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        String plugin = event.getPlugin().getName();
        rules.unregisterAll(plugin);
        buttons.removeAll(plugin);
    }
}
