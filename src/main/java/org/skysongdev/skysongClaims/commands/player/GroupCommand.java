package org.skysongdev.skysongClaims.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimGroup;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.LeafCommand;
import org.skysongdev.skysongClaims.commands.staff.StaffSupport;
import org.skysongdev.skysongClaims.group.GroupService;
import org.skysongdev.skysongClaims.group.Manager;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * {@code /sclaim group <id> <create|delete|rename|add|remove|list> ...}: the build groups of a
 * Claim. Any owner manages every group; staff manage any Claim's groups.
 */
final class GroupCommand extends LeafCommand {
    private static final List<String> ACTIONS = List.of("create", "delete", "rename", "add", "remove", "list");

    private final PlayerSupport support;
    private final GroupService groups;

    GroupCommand(PlayerSupport support, GroupService groups) {
        super(support.messenger(), "group", "<id> <create|delete|rename|add|remove|list> ...", 2, null);
        this.support = support;
        this.groups = groups;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Manager manager = sender.hasPermission(StaffSupport.ADMIN_PERMISSION)
                ? Manager.staff()
                : Manager.owner(support.characters().own(sender));
        String claimId = args[0];
        String action = args[1].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 2, args.length);
        switch (action) {
            case "create" -> {
                need(rest, 1, "create <name>");
                ClaimGroup group = groups.create(manager, claimId, rest[0]);
                messenger.send(sender, "group.created", unparsed("group", group.id()), unparsed("claim", claimId));
            }
            case "delete" -> {
                need(rest, 1, "delete <group>");
                groups.delete(manager, claimId, rest[0]);
                messenger.send(sender, "group.deleted", unparsed("group", rest[0]), unparsed("claim", claimId));
            }
            case "rename" -> {
                need(rest, 2, "rename <group> <name...>");
                ClaimGroup group = groups.rename(manager, claimId, rest[0],
                        String.join(" ", Arrays.copyOfRange(rest, 1, rest.length)));
                messenger.send(sender, "group.renamed", unparsed("group", group.id()), unparsed("name", group.displayName()));
            }
            case "add" -> {
                need(rest, 2, "add <group> <player> [profile]");
                CharacterId member = support.characters().character(rest[1], rest.length > 2 ? rest[2] : null);
                ClaimGroup group = groups.addMember(manager, claimId, rest[0], member);
                messenger.send(sender, "group.member-added", unparsed("character", ClaimArguments.describe(member)),
                        unparsed("group", group.id()));
            }
            case "remove" -> {
                need(rest, 2, "remove <group> <player> [profile]");
                CharacterId member = rest.length > 2
                        ? support.characters().character(rest[1], rest[2])
                        : onlyMember(claimId, rest[0], rest[1]);
                ClaimGroup group = groups.removeMember(manager, claimId, rest[0], member);
                messenger.send(sender, "group.member-removed", unparsed("character", ClaimArguments.describe(member)),
                        unparsed("group", group.id()));
            }
            case "list" -> list(sender, manager, claimId);
            default -> throw RefusedException.of("command.usage", "usage", "sclaim group " + usage());
        }
    }

    private void list(CommandSender sender, Manager manager, String claimId) {
        List<ClaimGroup> list = groups.list(manager, claimId);
        if (list.isEmpty()) {
            messenger.send(sender, "group.none", unparsed("claim", claimId));
            return;
        }
        for (ClaimGroup group : list) {
            messenger.send(sender, "group.line", unparsed("group", group.id()), unparsed("name", group.displayName()),
                    unparsed("members", group.members().isEmpty() ? "-" : group.members().stream()
                            .map(ClaimArguments::describe).collect(Collectors.joining(", "))));
        }
    }

    /** The player's character in the group; asks for the profile when there are several. */
    private CharacterId onlyMember(String claimId, String groupId, String playerName) {
        Claim claim = support.state().find(claimId).orElseThrow(() -> RefusedException.of("claim.unknown", "claim", claimId));
        List<CharacterId> matches = claim.group(groupId.toLowerCase(Locale.ROOT)).stream()
                .flatMap(group -> group.members().stream())
                .filter(member -> playerName.equalsIgnoreCase(Bukkit.getOfflinePlayer(member.player()).getName()))
                .toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            throw RefusedException.of("group.player-not-member", "player", playerName, "group", groupId);
        }
        throw RefusedException.of("claims.which-profile", "player", playerName);
    }

    private static void need(String[] rest, int count, String usage) {
        if (rest.length < count) {
            throw RefusedException.of("command.usage", "usage", "sclaim group <id> " + usage);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return sender.hasPermission(StaffSupport.ADMIN_PERMISSION)
                    ? ClaimArguments.startingWith(support.state().all().stream().map(Claim::id), args[0])
                    : support.ownIds(sender, args[0]);
        }
        if (args.length == 2) {
            return ClaimArguments.startingWith(ACTIONS.stream(), args[1]);
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3 && !action.equals("create") && !action.equals("list")) {
            return ClaimArguments.startingWith(support.state().find(args[0]).stream()
                    .flatMap(claim -> claim.groups().stream()).map(ClaimGroup::id), args[2]);
        }
        if ((action.equals("add") || action.equals("remove")) && args.length == 4) {
            return support.characters().onlinePlayers(args[3]);
        }
        if ((action.equals("add") || action.equals("remove")) && args.length == 5) {
            return support.characters().profiles(args[3], args[4]);
        }
        return Stream.<String>empty().toList();
    }
}
