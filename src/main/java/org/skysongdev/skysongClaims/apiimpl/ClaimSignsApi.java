package org.skysongdev.skysongClaims.apiimpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongClaims.api.ClaimSigns;
import org.skysongdev.skysongClaims.api.MenuButton;
import org.skysongdev.skysongClaims.api.SignInfo;
import org.skysongdev.skysongClaims.sign.ClaimSign;
import org.skysongdev.skysongClaims.sign.MenuButtons;
import org.skysongdev.skysongClaims.sign.SignService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** {@link ClaimSigns} over the {@link SignService} and the menu's plugin buttons. */
final class ClaimSignsApi implements ClaimSigns {
    private final ApiLookup lookup;
    private final SignService signs;
    private final MenuButtons buttons;

    ClaimSignsApi(ApiLookup lookup, SignService signs, MenuButtons buttons) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.signs = Objects.requireNonNull(signs, "signs");
        this.buttons = Objects.requireNonNull(buttons, "buttons");
    }

    /** @return what the API shows of a sign */
    static SignInfo info(ClaimSign sign) {
        return new SignInfo(sign.claimId(), sign.location().world(), sign.location().x(), sign.location().y(),
                sign.location().z());
    }

    @Override
    public @NotNull List<SignInfo> of(@NotNull String claimId) {
        lookup.claim(claimId);
        return signs.of(claimId).stream().map(ClaimSignsApi::info).toList();
    }

    @Override
    public void setPriceLine(@NotNull Plugin plugin, @NotNull String claimId, @NotNull Component line) {
        Objects.requireNonNull(plugin, "plugin");
        lookup.claim(claimId);
        signs.setPriceLine(claimId, Optional.of(GsonComponentSerializer.gson().serialize(Objects.requireNonNull(line, "line"))));
    }

    @Override
    public void clearPriceLine(@NotNull Plugin plugin, @NotNull String claimId) {
        Objects.requireNonNull(plugin, "plugin");
        lookup.claim(claimId);
        signs.setPriceLine(claimId, Optional.empty());
    }

    @Override
    public void addMenuButton(@NotNull Plugin plugin, @NotNull String id, @NotNull MenuButton button) {
        lookup.checkThread();
        buttons.add(plugin.getName(), Objects.requireNonNull(id, "id"), button);
    }

    @Override
    public void removeMenuButton(@NotNull Plugin plugin, @NotNull String id) {
        lookup.checkThread();
        buttons.remove(plugin.getName(), id);
    }
}
