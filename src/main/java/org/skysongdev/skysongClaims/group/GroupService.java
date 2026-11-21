package org.skysongdev.skysongClaims.group;

import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The groups owners make in their Claims to let other characters build. Every owner manages
 * every group. Groups can only give building rights: doors and chests are open to everyone anyway.
 */
public final class GroupService {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final ClaimStore store;
    private final Supplier<GroupLimits> limits;

    /** @param limits from config.yml; a supplier, so a reload takes effect at once */
    public GroupService(ClaimStore store, Supplier<GroupLimits> limits) {
        this.store = Objects.requireNonNull(store, "store");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Makes a group whose members may build. Its id is the name in lower case.
     *
     * @throws RefusedException when the name is invalid or taken, or the Claim has as many groups as allowed
     */
    public ClaimGroup create(Manager manager, String claimId, String name) {
        Claim claim = managed(manager, claimId);
        String id = name.toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            throw RefusedException.of("group.invalid-name", "group", name);
        }
        if (claim.group(id).isPresent()) {
            throw RefusedException.of("group.taken", "group", id);
        }
        int max = limits.get().groupsPerClaim();
        if (max > 0 && claim.groups().size() >= max) {
            throw RefusedException.of("group.too-many", "max", Integer.toString(max));
        }
        ClaimGroup group = new ClaimGroup(id, name, Set.of(GroupPermission.BUILD), Set.of());
        List<ClaimGroup> groups = new ArrayList<>(claim.groups());
        groups.add(group);
        store.save(claim.withGroups(groups), ChangeCause.COMMAND);
        return group;
    }

    /** @throws RefusedException when there is no such group */
    public void delete(Manager manager, String claimId, String groupId) {
        Claim claim = managed(manager, claimId);
        ClaimGroup group = requireGroup(claim, groupId);
        List<ClaimGroup> groups = new ArrayList<>(claim.groups());
        groups.remove(group);
        store.save(claim.withGroups(groups), ChangeCause.COMMAND);
    }

    /** Changes the name shown in lists; the id stays. */
    public ClaimGroup rename(Manager manager, String claimId, String groupId, String name) {
        Claim claim = managed(manager, claimId);
        ClaimGroup group = requireGroup(claim, groupId);
        if (name.isBlank()) {
            throw RefusedException.of("group.invalid-name", "group", name);
        }
        return replace(claim, new ClaimGroup(group.id(), name.strip(), group.permissions(), group.members()));
    }

    /**
     * Adds a character. Owners already build, so they can't be members.
     *
     * @throws RefusedException when it is an owner, already a member, or the group is full
     */
    public ClaimGroup addMember(Manager manager, String claimId, String groupId, CharacterId member) {
        Claim claim = managed(manager, claimId);
        ClaimGroup group = requireGroup(claim, groupId);
        if (claim.isOwnedBy(member)) {
            throw RefusedException.of("group.owner-member", "character", member.profile());
        }
        if (group.members().contains(member)) {
            throw RefusedException.of("group.already-member", "character", member.profile(), "group", group.id());
        }
        int max = limits.get().membersPerGroup();
        if (max > 0 && group.members().size() >= max) {
            throw RefusedException.of("group.full", "group", group.id(), "max", Integer.toString(max));
        }
        Set<CharacterId> members = new LinkedHashSet<>(group.members());
        members.add(member);
        return replace(claim, new ClaimGroup(group.id(), group.displayName(), group.permissions(), members));
    }

    /** @throws RefusedException when the character isn't a member */
    public ClaimGroup removeMember(Manager manager, String claimId, String groupId, CharacterId member) {
        Claim claim = managed(manager, claimId);
        ClaimGroup group = requireGroup(claim, groupId);
        if (!group.members().contains(member)) {
            throw RefusedException.of("group.not-member", "character", member.profile(), "group", group.id());
        }
        return replace(claim, group.withoutMember(member));
    }

    /** @return the Claim's groups, for one of its owners or staff */
    public List<ClaimGroup> list(Manager manager, String claimId) {
        return managed(manager, claimId).groups();
    }

    private Claim managed(Manager manager, String claimId) {
        Claim claim = store.require(claimId);
        manager.check(claim);
        return claim;
    }

    private static ClaimGroup requireGroup(Claim claim, String groupId) {
        return claim.group(groupId.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> RefusedException.of("group.unknown", "group", groupId, "claim", claim.id()));
    }

    private ClaimGroup replace(Claim claim, ClaimGroup changed) {
        List<ClaimGroup> groups = claim.groups().stream()
                .map(group -> group.id().equals(changed.id()) ? changed : group)
                .toList();
        store.save(claim.withGroups(groups), ChangeCause.COMMAND);
        return changed;
    }
}
