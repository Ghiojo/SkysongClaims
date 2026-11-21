package org.skysongdev.skysongClaims.ownership;

import org.skysongdev.skysongClaims.api.OwnerCause;
import org.skysongdev.skysongClaims.api.OwnershipCheck;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Co-owner invites. Any owner may invite a character, which must accept before the invite
 * expires. Invites live in memory only, so a restart drops them.
 */
public final class InviteService {
    /** An invite waiting for an answer. */
    public record Invite(String claimId, CharacterId inviter, CharacterId invitee, Instant expires) {
    }

    private final ClaimStore store;
    private final OwnershipService ownership;
    private final Supplier<Duration> expiry;
    private final Clock clock;
    private final List<Invite> invites = new ArrayList<>();

    /** @param expiry how long an invite lasts; a supplier, so a reload takes effect for new invites */
    public InviteService(ClaimStore store, OwnershipService ownership, Supplier<Duration> expiry, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.expiry = Objects.requireNonNull(expiry, "expiry");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * An owner invites a character to co-own the Claim. A new invite to the same Claim
     * replaces the old one.
     *
     * @throws RefusedException when the inviter isn't an owner, or the invitee couldn't become one now
     */
    public Invite invite(CharacterId inviter, String claimId, CharacterId invitee) {
        Claim claim = store.require(claimId);
        if (!claim.isOwnedBy(inviter)) {
            throw RefusedException.of("ownership.not-owner", "claim", claim.id());
        }
        OwnershipCheck check = ownership.canOwn(invitee, claim);
        if (check != OwnershipCheck.OK) {
            throw OwnershipService.refusal(check, claim, invitee);
        }
        dropExpired();
        invites.removeIf(invite -> invite.invitee().equals(invitee) && invite.claimId().equals(claim.id()));
        Invite invite = new Invite(claim.id(), inviter, invitee, clock.instant().plus(expiry.get()));
        invites.add(invite);
        return invite;
    }

    /**
     * The invitee becomes an owner, if they still may.
     *
     * @param claimId which invite, or empty when they have only one
     * @throws RefusedException when there is no such invite, or the Claim no longer has room for them
     */
    public Claim accept(CharacterId invitee, Optional<String> claimId) {
        Invite invite = take(invitee, claimId);
        return ownership.addOwner(invite.claimId(), invitee, OwnerCause.INVITE, ChangeCause.COMMAND, false);
    }

    /** @throws RefusedException when there is no such invite */
    public Invite deny(CharacterId invitee, Optional<String> claimId) {
        return take(invitee, claimId);
    }

    /** @return how long a new invite lasts */
    public Duration expiry() {
        return expiry.get();
    }

    /** @return the character's invites that haven't expired */
    public List<Invite> pending(CharacterId invitee) {
        dropExpired();
        return invites.stream().filter(invite -> invite.invitee().equals(invitee)).toList();
    }

    private Invite take(CharacterId invitee, Optional<String> claimId) {
        List<Invite> mine = pending(invitee).stream()
                .filter(invite -> claimId.map(invite.claimId()::equals).orElse(true))
                .toList();
        if (mine.isEmpty()) {
            throw RefusedException.of("ownership.no-invite");
        }
        if (mine.size() > 1) {
            throw RefusedException.of("ownership.which-invite", "claims",
                    String.join(", ", mine.stream().map(Invite::claimId).toList()));
        }
        invites.remove(mine.get(0));
        return mine.get(0);
    }

    private void dropExpired() {
        Instant now = clock.instant();
        invites.removeIf(invite -> !invite.expires().isAfter(now));
    }
}
