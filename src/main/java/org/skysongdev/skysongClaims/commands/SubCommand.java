package org.skysongdev.skysongClaims.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One node of a command tree, such as {@code create} under {@code /sclaims}. */
public interface SubCommand {
    /** The word that selects this command, in lower case. */
    String name();

    /** The permission needed to run this command, or {@code null} when its parent's is enough. */
    default @Nullable String permission() {
        return null;
    }

    /** Arguments shown in usage messages, e.g. {@code "<id> <type> <region...>"}. */
    String usage();

    /** Runs the command. {@code args} leaves out the words that selected it. */
    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    default boolean canUse(CommandSender sender) {
        String permission = permission();
        return permission == null || sender.hasPermission(permission);
    }
}
