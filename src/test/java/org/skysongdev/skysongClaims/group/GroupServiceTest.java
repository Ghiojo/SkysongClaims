package org.skysongdev.skysongClaims.group;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongClaims.api.ClaimGroup;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.GroupPermission;
import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupServiceTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Alice");
    private static final CharacterId BOB = new CharacterId(UUID.randomUUID(), "Bob");
    private static final CharacterId CAROL = new CharacterId(UUID.randomUUID(), "Carol");

    private final ClaimStore store = new ClaimStore(new ClaimState(), new InMemoryClaimRepository());
    private final AtomicReference<GroupLimits> limits = new AtomicReference<>(new GroupLimits(5, 0));
    private final GroupService groups = new GroupService(store, limits::get);
    private final Manager alice = Manager.owner(ALICE);

    @BeforeEach
    void setUp() {
        store.save(Claim.create("house", "other", false, 2, List.of(new ClaimRegion("world", "house")))
                .withOwners(List.of(new ClaimOwner(ALICE, Instant.EPOCH, OwnerCause.STAFF))), ChangeCause.COMMAND);
    }

    private static String refusal(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(RefusedException.class, action).messageKey();
    }

    private ClaimGroup family() {
        return store.require("house").group("family").orElseThrow();
    }

    @Test
    void aNewGroupMayBuild() {
        ClaimGroup group = groups.create(alice, "house", "Family");

        assertEquals("family", group.id());
        assertEquals("Family", group.displayName());
        assertEquals(Set.of(GroupPermission.BUILD), group.permissions());
        assertEquals(List.of(group), store.require("house").groups());
    }

    @Test
    void membersComeAndGo() {
        groups.create(alice, "house", "Family");

        groups.addMember(alice, "house", "FAMILY", BOB);
        groups.addMember(alice, "house", "family", CAROL);
        assertEquals(List.of(BOB, CAROL), List.copyOf(family().members()));

        groups.removeMember(alice, "house", "family", BOB);
        assertEquals(Set.of(CAROL), family().members());
        assertEquals("group.not-member", refusal(() -> groups.removeMember(alice, "house", "family", BOB)));
        assertEquals("group.already-member", refusal(() -> groups.addMember(alice, "house", "family", CAROL)));
    }

    @Test
    void ownersArentMembers() {
        groups.create(alice, "house", "Family");

        assertEquals("group.owner-member", refusal(() -> groups.addMember(alice, "house", "family", ALICE)));
    }

    @Test
    void onlyOwnersAndStaffManage() {
        assertEquals("ownership.not-owner", refusal(() -> groups.create(Manager.owner(BOB), "house", "Mine")));

        groups.create(Manager.staff(), "house", "Staff");
        assertEquals(1, groups.list(alice, "house").size());
    }

    @Test
    void limitsComeFromTheConfig() {
        limits.set(new GroupLimits(1, 1));
        groups.create(alice, "house", "Family");

        assertEquals("group.too-many", refusal(() -> groups.create(alice, "house", "Friends")));
        groups.addMember(alice, "house", "family", BOB);
        assertEquals("group.full", refusal(() -> groups.addMember(alice, "house", "family", CAROL)));
    }

    @Test
    void namesAreChecked() {
        groups.create(alice, "house", "Family");

        assertEquals("group.taken", refusal(() -> groups.create(alice, "house", "family")));
        assertEquals("group.invalid-name", refusal(() -> groups.create(alice, "house", "my family")));
        assertEquals("group.unknown", refusal(() -> groups.delete(alice, "house", "friends")));
    }

    @Test
    void renameKeepsTheIdAndDeleteRemoves() {
        groups.create(alice, "house", "Family");

        assertEquals("The Family", groups.rename(alice, "house", "family", " The Family ").displayName());
        groups.delete(alice, "house", "family");

        assertTrue(store.require("house").groups().isEmpty());
    }
}
