package org.skysongdev.skysongClaims.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.skysongdev.skysongClaims.claim.ClaimRegion;

import java.util.List;
import java.util.Optional;

/** {@link RegionGateway} on WorldGuard's region managers. */
public final class WorldGuardRegions implements RegionGateway {

    @Override
    public boolean exists(ClaimRegion region) {
        return region(region).isPresent();
    }

    @Override
    public List<String> regionIds(String world) {
        return manager(world).map(manager -> manager.getRegions().keySet().stream().sorted().toList()).orElse(List.of());
    }

    /** @return the WorldGuard region, when its world is loaded and it exists */
    Optional<ProtectedRegion> region(ClaimRegion region) {
        return manager(region.world()).flatMap(manager -> Optional.ofNullable(manager.getRegion(region.regionId())));
    }

    static Optional<RegionManager> manager(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)));
    }
}
