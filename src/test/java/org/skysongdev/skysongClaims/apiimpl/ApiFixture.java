package org.skysongdev.skysongClaims.apiimpl;

import org.skysongdev.skysongClaims.api.ClaimRegion;
import org.skysongdev.skysongClaims.api.event.ChangeCause;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimLocator;
import org.skysongdev.skysongClaims.claim.ClaimService;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.claim.InMemoryClaimRepository;
import org.skysongdev.skysongClaims.limit.LimitRules;
import org.skysongdev.skysongClaims.limit.Limits;
import org.skysongdev.skysongClaims.ownership.OwnershipService;
import org.skysongdev.skysongClaims.sign.InMemorySignRepository;
import org.skysongdev.skysongClaims.sign.MenuButtons;
import org.skysongdev.skysongClaims.sign.SignService;
import org.skysongdev.skysongClaims.sign.SignText;
import org.skysongdev.skysongClaims.support.FakeCharacters;
import org.skysongdev.skysongClaims.support.FakeRegions;
import org.skysongdev.skysongClaims.support.FakeSignBlocks;
import org.skysongdev.skysongClaims.support.MutableClock;
import org.skysongdev.skysongClaims.support.TestMessages;
import org.skysongdev.skysongClaims.support.TestTypes;

import java.util.List;

/**
 * The real API over in-memory features, with a Free {@code hovel_1} (2 slots) and a bought
 * {@code house} (1 slot), both unowned.
 */
final class ApiFixture {
    final ClaimState state = new ClaimState();
    final ClaimStore store = new ClaimStore(state, new InMemoryClaimRepository());
    final FakeRegions regions = new FakeRegions().add("world", "hovel_1", "house");
    final FakeCharacters characters = new FakeCharacters();
    final FakeSignBlocks blocks = new FakeSignBlocks();
    final LimitRules limitRules = new LimitRules((what, e) -> {
        throw e;
    });
    final MenuButtons menuButtons = new MenuButtons();
    final ClaimService claims = new ClaimService(store, TestTypes::registry, regions);
    final Limits limits = new Limits(state, limitRules);
    final OwnershipService ownership = new OwnershipService(store, TestTypes::registry, limits, characters, new MutableClock());
    final SignService signs = new SignService(state, new InMemorySignRepository(), blocks,
            new SignText(TestMessages.messenger(), TestTypes::registry), message -> {
            });
    boolean mainThread = true;
    final SkysongClaimsApiImpl api;

    ApiFixture() {
        store.observe(signs);
        store.save(Claim.create("hovel_1", "hovel", true, 2, List.of(new ClaimRegion("world", "hovel_1"))), ChangeCause.COMMAND);
        store.save(Claim.create("house", "other", false, 1, List.of(new ClaimRegion("world", "house"))), ChangeCause.COMMAND);
        api = new SkysongClaimsApiImpl(new SkysongClaimsApiImpl.Features(state, TestTypes::registry,
                new ClaimLocator(state, regions), claims, ownership, limits, limitRules, signs, menuButtons),
                new MainThread(() -> mainThread));
    }
}
