package org.skysongdev.skysongClaims.flag;

import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.group.Manager;
import org.skysongdev.skysongClaims.messaging.RefusedException;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.type.ClaimTypesParser;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The WorldGuard flags owners may set on their whole Claim, from {@code owner-flags} in
 * config.yml (by default {@code greeting} and {@code farewell}). {@code build} and the type's
 * open-use flags are always refused, even when listed, so owners can never lock doors or chests.
 */
public final class FlagService {
    private final ClaimStore store;
    private final Supplier<List<String>> ownerFlags;
    private final Supplier<ClaimTypeRegistry> types;
    private final RegionGateway regions;

    /** @param ownerFlags {@code owner-flags} from config.yml; a supplier, so a reload takes effect at once */
    public FlagService(ClaimStore store, Supplier<List<String>> ownerFlags, Supplier<ClaimTypeRegistry> types,
                       RegionGateway regions) {
        this.store = Objects.requireNonNull(store, "store");
        this.ownerFlags = Objects.requireNonNull(ownerFlags, "ownerFlags");
        this.types = Objects.requireNonNull(types, "types");
        this.regions = Objects.requireNonNull(regions, "regions");
    }

    /** @return the flags owners of this Claim may set */
    public List<String> editable(Claim claim) {
        List<String> protectedFlags = types.get().find(claim.typeId()).map(ClaimType::openUse)
                .orElse(ClaimTypesParser.DEFAULT_OPEN_USE);
        return ownerFlags.get().stream()
                .filter(flag -> !flag.equals("build") && !protectedFlags.contains(flag))
                .toList();
    }

    /**
     * Sets the flag on every region of the Claim.
     *
     * @throws RefusedException when owners may not set it, or WorldGuard doesn't take the value
     */
    public Claim set(Manager manager, String claimId, String flagName, String value) {
        Claim claim = managed(manager, claimId);
        String flag = requireEditable(claim, flagName);
        regions.checkFlag(flag, value).ifPresent(problem -> {
            throw RefusedException.of("flag.bad-value", "flag", flag, "problem", problem);
        });
        Map<String, String> flags = new LinkedHashMap<>(claim.flags());
        flags.put(flag, value);
        return save(claim.withFlags(flags));
    }

    /** Removes the flag from every region of the Claim. */
    public Claim clear(Manager manager, String claimId, String flagName) {
        Claim claim = managed(manager, claimId);
        String flag = requireEditable(claim, flagName);
        Map<String, String> flags = new LinkedHashMap<>(claim.flags());
        flags.remove(flag);
        return save(claim.withFlags(flags));
    }

    private Claim managed(Manager manager, String claimId) {
        Claim claim = store.require(claimId);
        manager.check(claim);
        return claim;
    }

    private String requireEditable(Claim claim, String flagName) {
        String flag = flagName.toLowerCase(Locale.ROOT);
        if (!editable(claim).contains(flag)) {
            throw RefusedException.of("flag.not-editable", "flag", flag, "flags", String.join(", ", editable(claim)));
        }
        return flag;
    }

    private Claim save(Claim claim) {
        store.save(claim, ChangeCause.COMMAND);
        return claim;
    }
}
