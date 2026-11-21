package org.skysongdev.skysongClaims.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.skysongdev.skysongClaims.api.ClaimRegion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    public List<ClaimRegion> regionsAt(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return List.of();
        }
        return manager(world.getName())
                .map(manager -> manager.getApplicableRegions(BukkitAdapter.asBlockVector(location)).getRegions().stream()
                        .sorted(Comparator.comparingInt(ProtectedRegion::getPriority).reversed())
                        .map(region -> new ClaimRegion(world.getName(), region.getId()))
                        .toList())
                .orElse(List.of());
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
        access.flags().forEach((name, input) -> {
            Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
            try {
                if (flag == null) {
                    throw new InvalidFlagFormat("unknown flag");
                }
                setParsed(region, flag, input);
            } catch (InvalidFlagFormat e) {
                skipped.add(name);
            }
        });
        return skipped;
    }

    @Override
    public void clearFlag(ClaimRegion claimRegion, String name) {
        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
        if (flag == null) {
            return;
        }
        region(claimRegion).filter(region -> region.getFlag(flag) != null).ifPresent(region -> region.setFlag(flag, null));
    }

    @Override
    public Optional<String> checkFlag(String name, String input) {
        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
        if (flag == null) {
            return Optional.of("WorldGuard has no flag " + name);
        }
        try {
            flag.parseInput(context(input));
            return Optional.empty();
        } catch (InvalidFlagFormat e) {
            return Optional.of(e.getMessage());
        }
    }

    private static <V> void setParsed(ProtectedRegion region, Flag<V> flag, String input) throws InvalidFlagFormat {
        V value = flag.parseInput(context(input));
        if (!Objects.equals(region.getFlag(flag), value)) {
            region.setFlag(flag, value);
        }
    }

    private static FlagContext context(String input) {
        return FlagContext.create().setInput(input).build();
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
