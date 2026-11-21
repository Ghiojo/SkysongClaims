package org.skysongdev.skysongClaims.apiimpl;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Guards the API methods that touch Claims: the services behind them are not thread-safe, so a
 * call from another thread is refused instead of racing them.
 */
public final class MainThread {
    private final BooleanSupplier isMainThread;

    public MainThread(BooleanSupplier isMainThread) {
        this.isMainThread = Objects.requireNonNull(isMainThread, "isMainThread");
    }

    /** @throws IllegalStateException when called off the server's main thread */
    public void check() {
        if (!isMainThread.getAsBoolean()) {
            throw new IllegalStateException("The SkysongClaims API must be called on the server's main thread."
                    + " Use Bukkit.getScheduler().runTask(...) to get back to it.");
        }
    }
}
