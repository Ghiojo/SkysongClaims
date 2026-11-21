package org.skysongdev.skysongClaims.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongClaims.messaging.Messenger;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * A command whose first argument picks one of its children, e.g. {@code /sclaims <create|delete|...>}.
 * It checks the child's permission and handles usage, unknown words and tab completion, so the
 * children only hold their own logic.
 */
public class CommandGroup implements SubCommand {
    private final String name;
    private final @Nullable String permission;
    private final Messenger messenger;
    private final Map<String, SubCommand> children = new LinkedHashMap<>();

    public CommandGroup(String name, @Nullable String permission, Messenger messenger) {
        this.name = name;
        this.permission = permission;
        this.messenger = messenger;
    }

    public final CommandGroup register(SubCommand child) {
        children.put(child.name(), child);
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public @Nullable String permission() {
        return permission;
    }

    @Override
    public String usage() {
        return "<" + String.join("|", children.keySet()) + ">";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messenger.send(sender, "command.usage", unparsed("usage", name + " " + usage()));
            return;
        }
        SubCommand child = children.get(args[0].toLowerCase(Locale.ROOT));
        if (child == null) {
            messenger.send(sender, "command.unknown-subcommand",
                    unparsed("input", args[0]), unparsed("usage", name + " " + usage()));
            return;
        }
        if (!child.canUse(sender)) {
            messenger.send(sender, "command.no-permission");
            return;
        }
        child.execute(sender, tail(args));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            return children.values().stream()
                    .filter(child -> child.canUse(sender))
                    .map(SubCommand::name)
                    .filter(childName -> childName.startsWith(typed))
                    .toList();
        }
        SubCommand child = children.get(typed);
        if (child == null || !child.canUse(sender)) {
            return List.of();
        }
        return child.tabComplete(sender, tail(args));
    }

    private static String[] tail(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }
}
