package org.skysongdev.skysongClaims.commands.staff;

import org.skysongdev.skysongClaims.claim.ClaimService;
import org.skysongdev.skysongClaims.claim.ClaimState;
import org.skysongdev.skysongClaims.commands.ClaimArguments;
import org.skysongdev.skysongClaims.messaging.Messenger;
import org.skysongdev.skysongClaims.type.ClaimTypeRegistry;

import java.util.function.Supplier;

/** What every {@code /sclaims} command uses. */
public record StaffSupport(
        ClaimService claims,
        ClaimState state,
        Supplier<ClaimTypeRegistry> types,
        ClaimArguments arguments,
        Messenger messenger) {

    /** Lets staff use every {@code /sclaims} command and do everything in any Claim. */
    public static final String ADMIN_PERMISSION = "skysongclaims.admin";
}
