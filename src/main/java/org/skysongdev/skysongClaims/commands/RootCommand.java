package org.skysongdev.skysongClaims.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Connects a command tree to a plugin.yml command such as {@code /sclaims}. */
public final class RootCommand implements TabExecutor {
    private final SubCommand tree;

    public RootCommand(SubCommand tree) {
        this.tree = tree;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        // plugin.yml holds the command's own permission, so Bukkit has checked it already.
        tree.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        return tree.tabComplete(sender, args);
    }
}
