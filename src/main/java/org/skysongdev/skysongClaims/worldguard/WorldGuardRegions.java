package org.skysongdev.skysongClaims.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.skysongdev.skysongClaims.claim.ClaimRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** {@link RegionGateway} on WorldGuard's region managers. WorldGuard saves the changed regions itself. */
public final class WorldGuardRegions implements RegionGateway {

    @Override
    public boolean exists(ClaimRegion region) {
        return region(region).isPresent();
    }

    @Override
    public List<String> regionIds(String world) {
        return manager(world).map(manager -> manager.getRegions().keySet().stream().sorted().toList()).orElse(List.of());
    }

    @Override
    public List<String> apply(ClaimRegion claimRegion, RegionAccess access) {
        Optional<ProtectedRegion> found = region(claimRegion);
        if (found.isEmpty()) {
            return List.of();
        }
        ProtectedRegion region = found.get();
        replace(region.getOwners(), access.owners());
        replace(region.getMembers(), access.members());
        List<String> skipped = new ArrayList<>();
        for (String name : access.openUse()) {
            Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
            if (!(flag instanceof StateFlag stateFlag)) {
                skipped.add(name);
                continue;
            }
            if (region.getFlag(stateFlag) != StateFlag.State.ALLOW) {
                region.setFlag(stateFlag, StateFlag.State.ALLOW);
            }
        }
        return skipped;
    }

    /** Only touches the domain when it differs, so WorldGuard doesn't save unchanged regions. */
    private static void replace(DefaultDomain domain, Set<UUID> players) {
        boolean same = domain.getGroups().isEmpty() && domain.getPlayers().isEmpty()
                && domain.getUniqueIds().equals(players);
        if (same) {
            return;
        }
        domain.clear();
        players.forEach(domain::addPlayer);
    }

    private static Optional<ProtectedRegion> region(ClaimRegion region) {
        return manager(region.world()).flatMap(manager -> Optional.ofNullable(manager.getRegion(region.regionId())));
    }

    private static Optional<RegionManager> manager(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)));
    }
}
