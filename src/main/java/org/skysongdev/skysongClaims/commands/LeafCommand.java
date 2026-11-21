package org.skysongdev.skysongClaims.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.messaging.RefusedException;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * A command that does one thing. It shows the usage when too few arguments were given, and
 * turns a {@link RefusedException} into its message, so {@link #run} only holds the action.
 */
public abstract class LeafCommand implements SubCommand {
    protected final Messenger messenger;
    private final String name;
    private final String usage;
    private final int minArgs;
    private final @Nullable String permission;

    protected LeafCommand(Messenger messenger, String name, String usage, int minArgs, @Nullable String permission) {
        this.messenger = messenger;
        this.name = name;
        this.usage = usage;
        this.minArgs = minArgs;
        this.permission = permission;
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final String usage() {
        return usage;
    }

    @Override
    public final @Nullable String permission() {
        return permission;
    }

    @Override
    public final void execute(CommandSender sender, String[] args) {
        if (args.length < minArgs) {
            messenger.send(sender, "command.usage", unparsed("usage", name + " " + usage));
            return;
        }
        try {
            run(sender, args);
        } catch (RefusedException refusal) {
            messenger.send(sender, refusal);
        }
    }

    /** Runs the command with at least {@code minArgs} arguments. */
    protected abstract void run(CommandSender sender, String[] args);
}
