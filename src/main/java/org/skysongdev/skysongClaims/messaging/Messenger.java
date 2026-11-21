package org.skysongdev.skysongClaims.messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Sends the texts of {@code messages.yml} by key, after the configured prefix.
 * <p>
 * Values that players typed (names, profiles, ids) must be passed as
 * {@link Placeholder#unparsed unparsed placeholders} so they can never inject MiniMessage
 * tags. Names staff wrote in MiniMessage, such as a Claim's display name, are parsed.
 */
public final class Messenger {
    private final MiniMessage miniMessage;
    private final Supplier<MessageCatalog> catalog;

    /** @param catalog the texts in use; a supplier, so a reload takes effect at once */
    public Messenger(MiniMessage miniMessage, Supplier<MessageCatalog> catalog) {
        this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /** Sends the text {@code key} after the prefix. */
    public void send(CommandSender recipient, String key, TagResolver... placeholders) {
        MessageCatalog texts = catalog.get();
        recipient.sendMessage(miniMessage.deserialize(texts.text(MessageCatalog.PREFIX) + texts.text(key), placeholders));
    }

    /** Sends the message of a refused action, with its placeholders as plain text. */
    public void send(CommandSender recipient, RefusedException refusal) {
        send(recipient, refusal.messageKey(), unparsed(refusal.placeholders()));
    }

    /** @return the text {@code key} as a component, without the prefix */
    public Component render(String key, TagResolver... placeholders) {
        return miniMessage.deserialize(catalog.get().text(key), placeholders);
    }

    /** @return one unparsed placeholder per entry, so typed values can't inject tags */
    public static TagResolver[] unparsed(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
    }
}
