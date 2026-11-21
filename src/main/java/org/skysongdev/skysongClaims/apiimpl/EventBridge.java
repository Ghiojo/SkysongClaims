package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.event.Event;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.event.ClaimCreateEvent;
import org.skysongdev.skysongClaims.api.event.ClaimDeleteEvent;
import org.skysongdev.skysongClaims.api.event.ClaimOwnerAddEvent;
import org.skysongdev.skysongClaims.api.event.ClaimOwnerRemoveEvent;
import org.skysongdev.skysongClaims.api.event.ClaimSaleChangeEvent;
import org.skysongdev.skysongClaims.api.event.ClaimSignChangeEvent;
import org.skysongdev.skysongClaims.api.event.ClaimTypeChangeEvent;
import org.skysongdev.skysongClaims.api.event.SkysongClaimsReloadEvent;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimChange;
import org.skysongdev.skysongClaims.claim.ClaimObserver;
import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.SignService;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Turns internal changes into the API's Bukkit events, by comparing each Claim before and after
 * a change. Removals fire before additions, so a transfer reads as "from left, then to joined".
 */
public final class EventBridge implements ClaimObserver, SignService.Listener {
    private final Consumer<Event> fire;

    /** @param fire calls an event, normally {@code Bukkit.getPluginManager()::callEvent} */
    public EventBridge(Consumer<Event> fire) {
        this.fire = Objects.requireNonNull(fire, "fire");
    }

    @Override
    public void claimChanged(ClaimChange change) {
        if (change.isCreate()) {
            fire.accept(new ClaimCreateEvent(change.after().orElseThrow().info(), change.cause()));
            return;
        }
        Claim before = change.before().orElseThrow();
        if (change.isDelete()) {
            fire.accept(new ClaimDeleteEvent(before.info(), change.cause()));
            return;
        }
        Claim after = change.after().orElseThrow();
        ClaimInfo info = after.info();
        Set<CharacterId> owned = characters(before.owners());
        Set<CharacterId> owns = characters(after.owners());
        for (CharacterId gone : owned) {
            if (!owns.contains(gone)) {
                fire.accept(new ClaimOwnerRemoveEvent(info, gone, change.cause()));
            }
        }
        for (CharacterId joined : owns) {
            if (!owned.contains(joined)) {
                fire.accept(new ClaimOwnerAddEvent(info, joined, change.cause()));
            }
        }
        if (!before.saleSetBy().equals(after.saleSetBy())) {
            fire.accept(new ClaimSaleChangeEvent(info, change.cause()));
        }
        if (!before.typeId().equals(after.typeId())) {
            fire.accept(new ClaimTypeChangeEvent(info, before.typeId(), change.cause()));
        }
    }

    @Override
    public void signChanged(ClaimSign sign, boolean linked) {
        fire.accept(new ClaimSignChangeEvent(ClaimSignsApi.info(sign), linked));
    }

    /** Fires {@link SkysongClaimsReloadEvent}; call it after the config reloaded. */
    public void reloaded() {
        fire.accept(new SkysongClaimsReloadEvent());
    }

    private static Set<CharacterId> characters(List<ClaimOwner> owners) {
        return owners.stream().map(ClaimOwner::character).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
