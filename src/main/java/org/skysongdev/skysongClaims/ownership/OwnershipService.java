package org.skysongdev.skysongClaims.ownership;

import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.character.CharacterDirectory;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimOwner;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.OwnerCause;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongstats.api.CharacterId;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Who owns which Claim. Owners are equal: none of them can remove another, so the only ways
 * out are leaving, or staff and other plugins removing them.
 */
public final class OwnershipService {
    private final ClaimStore store;
    private final Supplier<ClaimTypeRegistry> types;
    private final Limits limits;
    private final CharacterDirectory characters;
    private final Clock clock;

    public OwnershipService(ClaimStore store, Supplier<ClaimTypeRegistry> types, Limits limits,
                            CharacterDirectory characters, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.types = Objects.requireNonNull(types, "types");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.characters = Objects.requireNonNull(characters, "characters");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** @return whether the character may become an owner now; a Claim whose type is gone has a limit of 0 */
    public OwnershipCheck canOwn(CharacterId character, Claim claim) {
        if (claim.isOwnedBy(character)) {
            return OwnershipCheck.ALREADY_OWNER;
        }
        if (!characters.exists(character)) {
            return OwnershipCheck.UNKNOWN_CHARACTER;
        }
        if (claim.openSlots() == 0) {
            return OwnershipCheck.FULL;
        }
        Optional<ClaimType> type = types.get().find(claim.typeId());
        if (type.isEmpty() || limits.remaining(Limits.subject(character, type.get()), type.get()) == 0) {
            return OwnershipCheck.TYPE_LIMIT;
        }
        return OwnershipCheck.OK;
    }

    /**
     * A player takes one slot of a Free Claim for the character they play.
     *
     * @throws RefusedException when it isn't a Free Claim, or {@link #canOwn} refuses
     */
    public Claim take(CharacterId character, String claimId) {
        Claim claim = store.require(claimId);
        if (!claim.free()) {
            throw RefusedException.of("ownership.not-free", "claim", claim.id());
        }
        return add(claim, character, OwnerCause.FREE_CLAIM, ChangeCause.FREE_CLAIM, false);
    }

    /**
     * Makes the character an owner, for staff and other plugins.
     *
     * @param force skip the type limit (never the slots or an unknown character)
     * @throws RefusedException when {@link #canOwn} refuses
     */
    public Claim addOwner(String claimId, CharacterId character, OwnerCause how, ChangeCause cause, boolean force) {
        return add(store.require(claimId), character, how, cause, force);
    }

    /**
     * The character stops owning the Claim. When it was the last owner the Claim is unowned
     * again: a Free Claim opens every slot, and its groups are cleared.
     *
     * @throws RefusedException {@code ownership.not-owner} when it isn't an owner
     */
    public Claim removeOwner(String claimId, CharacterId character, ChangeCause cause) {
        Claim claim = store.require(claimId);
        if (!claim.isOwnedBy(character)) {
            throw RefusedException.of("ownership.not-owner", "claim", claim.id());
        }
        Claim changed = claim.withoutCharacter(character);
        store.save(changed, cause);
        return changed;
    }

    /** @return the Claims the character owns */
    public List<Claim> ownedBy(CharacterId character) {
        return store.state().ownedBy(character);
    }

    private Claim add(Claim claim, CharacterId character, OwnerCause how, ChangeCause cause, boolean force) {
        OwnershipCheck check = canOwn(character, claim);
        if (check != OwnershipCheck.OK && !(force && check == OwnershipCheck.TYPE_LIMIT)) {
            throw RefusedException.of(check.messageKey(), "claim", claim.id(), "character", character.profile(),
                    "type", claim.typeId());
        }
        List<ClaimOwner> owners = new ArrayList<>(claim.owners());
        owners.add(new ClaimOwner(character, clock.instant(), how));
        Claim changed = claim.withOwners(owners);
        store.save(changed, cause);
        return changed;
    }
}
