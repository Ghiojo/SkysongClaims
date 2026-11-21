package org.skysongdev.skysongClaims.sign;

import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimChange;
import org.skysongdev.skysongClaims.claim.ClaimObserver;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.messaging.RefusedException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Claim signs: staff link a sign to a Claim, and the plugin keeps its lines current whenever the
 * Claim changes, its chunk loads, or the config reloads. A Claim can have several signs, and a
 * sign can stand anywhere. A sign whose block is gone is unlinked and logged.
 */
public final class SignService implements ClaimObserver {
    /** Told when a sign is linked ({@code linked} true) or unlinked. */
    @FunctionalInterface
    public interface Listener {
        void signChanged(ClaimSign sign, boolean linked);
    }

    private final ClaimState claims;
    private final SignRepository repository;
    private final SignBlocks blocks;
    private final SignText text;
    private final Consumer<String> log;
    private final Map<SignLocation, ClaimSign> signs = new LinkedHashMap<>();
    private final List<Listener> listeners = new ArrayList<>();

    /** @param log told about each sign unlinked because its block is gone */
    public SignService(ClaimState claims, SignRepository repository, SignBlocks blocks, SignText text, Consumer<String> log) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.text = Objects.requireNonNull(text, "text");
        this.log = Objects.requireNonNull(log, "log");
    }

    /** Loads the saved signs. Call once at start-up, before {@link #redrawAll}. */
    public void load() {
        signs.clear();
        for (ClaimSign sign : repository.loadAll()) {
            signs.put(sign.location(), sign);
        }
    }

    public void listen(Listener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Makes the sign show the Claim, replacing any Claim it showed before.
     *
     * @throws RefusedException when the Claim doesn't exist or no sign stands there
     */
    public ClaimSign link(String claimId, SignLocation location) {
        Claim claim = claims.find(claimId).orElseThrow(() -> RefusedException.of("claim.unknown", "claim", claimId));
        if (!blocks.isSign(location)) {
            throw RefusedException.of("signs.not-a-sign");
        }
        Optional.ofNullable(signs.get(location)).ifPresent(old -> unlink(old.location()));
        ClaimSign sign = new ClaimSign(claim.id(), location, Optional.empty());
        signs.put(location, sign);
        repository.save(sign);
        draw(claim, sign);
        listeners.forEach(listener -> listener.signChanged(sign, true));
        return sign;
    }

    /** @throws RefusedException {@code signs.not-linked} when that sign shows no Claim */
    public ClaimSign unlink(SignLocation location) {
        ClaimSign sign = Optional.ofNullable(signs.remove(location))
                .orElseThrow(() -> RefusedException.of("signs.not-linked"));
        repository.delete(location);
        blocks.release(location);
        listeners.forEach(listener -> listener.signChanged(sign, false));
        return sign;
    }

    public Optional<ClaimSign> at(SignLocation location) {
        return Optional.ofNullable(signs.get(location));
    }

    public List<ClaimSign> of(String claimId) {
        return signs.values().stream().filter(sign -> sign.claimId().equals(claimId)).toList();
    }

    /** Sets the fourth line of every sign of the Claim, as Adventure JSON; empty blanks it. */
    public void setPriceLine(String claimId, Optional<String> json) {
        Claim claim = claims.find(claimId).orElseThrow(() -> RefusedException.of("claim.unknown", "claim", claimId));
        for (ClaimSign sign : of(claimId)) {
            ClaimSign changed = sign.withPriceLine(json);
            signs.put(changed.location(), changed);
            repository.save(changed);
            draw(claim, changed);
        }
    }

    /** Rewrites every sign whose chunk is loaded, e.g. at start-up or after a reload. */
    public void redrawAll() {
        List.copyOf(signs.values()).forEach(this::redraw);
    }

    /** Rewrites the signs in a chunk that just loaded. */
    public void redrawChunk(String world, int chunkX, int chunkZ) {
        List.copyOf(signs.values()).stream()
                .filter(sign -> sign.location().inChunk(world, chunkX, chunkZ))
                .forEach(this::redraw);
    }

    @Override
    public void claimChanged(ClaimChange change) {
        if (change.isDelete()) {
            // The database drops them with the Claim; the blocks become normal signs again.
            for (ClaimSign sign : of(change.claimId())) {
                signs.remove(sign.location());
                blocks.release(sign.location());
                listeners.forEach(listener -> listener.signChanged(sign, false));
            }
            return;
        }
        of(change.claimId()).forEach(this::redraw);
    }

    private void redraw(ClaimSign sign) {
        Optional<Claim> claim = claims.find(sign.claimId());
        if (claim.isEmpty()) {
            signs.remove(sign.location());
            repository.delete(sign.location());
            return;
        }
        draw(claim.get(), sign);
    }

    private void draw(Claim claim, ClaimSign sign) {
        if (blocks.write(sign.location(), text.lines(claim, sign)) == SignBlocks.Result.NOT_A_SIGN) {
            signs.remove(sign.location());
            repository.delete(sign.location());
            log.accept("Unlinked the sign of claim " + claim.id() + " at " + sign.location() + ": the sign is gone");
            listeners.forEach(listener -> listener.signChanged(sign, false));
        }
    }
}
