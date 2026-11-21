package org.skysongdev.skysongClaims;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongClaims.apiimpl.ApiRegistration;
import org.skysongdev.skysongClaims.apiimpl.SkysongClaimsApiImpl;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.claim.ClaimService;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.claim.ClaimStore;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.commands.RootCommand;
import org.skysongdev.skysongClaims.commands.SubCommand;
import org.skysongdev.skysongClaims.commands.staff.StaffCommand;
import org.skysongdev.skysongClaims.commands.staff.StaffSupport;
import org.skysongdev.skysongClaims.config.ConfigException;
import org.skysongdev.skysongClaims.config.ConfigHolder;
import org.skysongdev.skysongClaims.config.ConfigLoader;
import org.skysongdev.skysongClaims.config.ConfigSection;
import org.skysongdev.skysongClaims.config.YamlFiles;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.storage.ClaimStorage;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;
import org.skysongdev.skysongClaims.worldguard.RegionGateway;
import org.skysongdev.skysongClaims.worldguard.WorldGuardRegions;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * The plugin class. It only builds the features and connects them; every rule lives in the
 * feature classes.
 */
public final class SkysongClaims extends JavaPlugin {
    private ClaimStorage storage;
    private ApiRegistration api;

    @Override
    public void onEnable() {
        ConfigHolder config;
        try {
            YamlFiles files = new YamlFiles(getDataFolder().toPath(), path -> saveResource(path, false));
            config = new ConfigHolder(new ConfigLoader(files, this::bundled, getLogger()::warning)::load);
        } catch (ConfigException e) {
            disable("its config is not valid: " + e.getMessage(), null);
            return;
        }
        getLogger().info("Loaded " + config.current().types().all().size() + " claim types");

        ClaimState state = new ClaimState();
        try {
            storage = ClaimStorage.open(config.current().settings().database(), getLogger());
            state.load(storage.claims().loadAll());
        } catch (RuntimeException e) {
            disable("it can't use its database (" + config.current().settings().database() + ")", e);
            return;
        }
        getLogger().info("Loaded " + state.all().size() + " claims");

        Supplier<ClaimTypeRegistry> types = () -> config.current().types();
        Messenger messenger = new Messenger(MiniMessage.miniMessage(), () -> config.current().messages());
        RegionGateway regions = new WorldGuardRegions();
        ClaimStore store = new ClaimStore(state, storage.claims());
        ClaimService claims = new ClaimService(store, types, regions);
        warnAboutUnknownTypes(claims);

        StaffSupport staff = new StaffSupport(claims, state, types, new ClaimArguments(state, types, regions), messenger);
        registerCommand("sclaims", new StaffCommand(staff, () -> {
            config.reload();
            warnAboutUnknownTypes(claims);
        }));

        // The API is offered last, once everything behind it is running.
        api = new ApiRegistration(getServer().getServicesManager(), new SkysongClaimsApiImpl());
        api.register(this);
    }

    private void warnAboutUnknownTypes(ClaimService claims) {
        for (Claim claim : claims.withUnknownType()) {
            getLogger().warning("Claim " + claim.id() + " has the type " + claim.typeId() + ", which is not in types.yml");
        }
    }

    private void registerCommand(String name, SubCommand tree) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), () -> "plugin.yml has no command " + name);
        RootCommand root = new RootCommand(tree);
        command.setExecutor(root);
        command.setTabCompleter(root);
    }

    /** Reads a default file from the plugin jar. */
    private ConfigSection bundled(String path) {
        InputStream in = Objects.requireNonNull(getResource(path), () -> "The jar has no " + path);
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlFiles.parse(path, reader);
        } catch (java.io.IOException e) {
            throw new ConfigException(path + ": can't be read from the jar (" + e.getMessage() + ")");
        }
    }

    private void disable(String reason, Throwable cause) {
        getLogger().log(Level.SEVERE, "SkysongClaims is disabled because " + reason, cause);
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        // Taken back first, so no plugin calls into features that are shutting down.
        if (api != null) {
            api.unregister();
        }
        // Last, so every change made while shutting down is still saved.
        if (storage != null) {
            storage.close();
            storage = null;
        }
    }
}
