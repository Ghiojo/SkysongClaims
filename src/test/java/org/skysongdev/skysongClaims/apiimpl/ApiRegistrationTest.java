package org.skysongdev.skysongClaims.apiimpl;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongClaims.api.SkysongClaimsApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRegistrationTest {
    private ServicesManager services;
    private Plugin plugin;
    private final SkysongClaimsApi api = new SkysongClaimsApiImpl();

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        services = server.getServicesManager();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void noApiBeforeRegistering() {
        IllegalStateException refusal = assertThrows(IllegalStateException.class, () -> SkysongClaimsApi.get(services));
        assertTrue(refusal.getMessage().contains("depend: [SkysongClaims]"));
    }

    @Test
    void registeredApiIsFound() {
        ApiRegistration registration = new ApiRegistration(services, api);

        registration.register(plugin);

        assertSame(api, SkysongClaimsApi.get(services));
        assertTrue(registration.isRegistered());
    }

    @Test
    void registeringTwiceRegistersOnce() {
        ApiRegistration registration = new ApiRegistration(services, api);

        registration.register(plugin);
        registration.register(plugin);

        assertEquals(1, services.getRegistrations(SkysongClaimsApi.class).size());
    }

    @Test
    void unregisteredApiIsGone() {
        ApiRegistration registration = new ApiRegistration(services, api);
        registration.register(plugin);

        registration.unregister();

        assertThrows(IllegalStateException.class, () -> SkysongClaimsApi.get(services));
        assertFalse(registration.isRegistered());
    }

    @Test
    void unregisteringWithoutRegisteringDoesNothing() {
        ApiRegistration registration = new ApiRegistration(services, api);

        registration.unregister();

        assertFalse(registration.isRegistered());
    }

    @Test
    void reportsItsVersion() {
        assertEquals(SkysongClaimsApiImpl.API_VERSION, api.apiVersion());
    }
}
