package org.skysongdev.skysongClaims.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.skysongdev.skysongClaims.claim.Claim;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.type.ClaimType;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * The four lines of a Claim sign: its name, its type, how many slots are open (Free Claims) or
 * whether it is for sale or owned (other Claims), and the price line another plugin set. The
 * first three come from {@code messages.yml}, so they can be styled.
 */
public final class SignText {
    private final Messenger messenger;
    private final Supplier<ClaimTypeRegistry> types;

    public SignText(Messenger messenger, Supplier<ClaimTypeRegistry> types) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.types = Objects.requireNonNull(types, "types");
    }

    public List<Component> lines(Claim claim, ClaimSign sign) {
        String typeName = types.get().find(claim.typeId()).map(ClaimType::displayName).orElse(claim.typeId());
        return List.of(
                messenger.render("signs.line-name", parsed("name", claim.displayName())),
                messenger.render("signs.line-type", parsed("type_name", typeName)),
                status(claim),
                sign.priceLine().map(GsonComponentSerializer.gson()::deserialize).orElse(Component.empty()));
    }

    private Component status(Claim claim) {
        if (claim.free()) {
            return claim.hasRoom()
                    ? messenger.render("signs.free-open", unparsed("open", Integer.toString(claim.openSlots().orElseThrow())),
                            unparsed("slots", Integer.toString(claim.slots().orElseThrow())))
                    : messenger.render("signs.free-full");
        }
        if (claim.isForSale()) {
            return messenger.render("signs.for-sale");
        }
        return messenger.render(claim.isOwned() ? "signs.owned" : "signs.unowned");
    }
}
