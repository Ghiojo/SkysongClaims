package org.skysongdev.skysongClaims.claim;

import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.messaging.RefusedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The one way services change Claims: it updates memory, saves, then tells the observers, so
 * no feature can forget a step. Every service shares one store.
 */
public final class ClaimStore {
    private final ClaimState state;
    private final ClaimRepository repository;
    private final List<ClaimObserver> observers = new ArrayList<>();

    public ClaimStore(ClaimState state, ClaimRepository repository) {
        this.state = Objects.requireNonNull(state, "state");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** @return the Claims, to read */
    public ClaimState state() {
        return state;
    }

    public void observe(ClaimObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    /** @throws RefusedException {@code claim.unknown} when no Claim has that id */
    public Claim require(String id) {
        return state.find(id).orElseThrow(() -> RefusedException.of("claim.unknown", "claim", id));
    }

    /** Saves a new or changed Claim and tells the observers. */
    public void save(Claim claim, ChangeCause cause) {
        Optional<Claim> before = state.find(claim.id());
        if (before.isPresent() && before.get().equals(claim)) {
            return;
        }
        state.put(claim);
        repository.save(claim);
        notify(new ClaimChange(before, Optional.of(claim), cause));
    }

    /** Deletes a Claim and tells the observers. Does nothing when it doesn't exist. */
    public void delete(String id, ChangeCause cause) {
        Optional<Claim> before = state.find(id);
        if (before.isEmpty()) {
            return;
        }
        state.remove(id);
        repository.delete(id);
        notify(new ClaimChange(before, Optional.empty(), cause));
    }

    private void notify(ClaimChange change) {
        observers.forEach(observer -> observer.claimChanged(change));
    }
}
