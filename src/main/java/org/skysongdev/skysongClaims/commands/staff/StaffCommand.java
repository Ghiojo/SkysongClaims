package org.skysongdev.skysongClaims.commands.staff;

import org.skysongdev.skysongClaims.commands.CommandGroup;

/** {@code /sclaims}: the staff commands. Every one needs {@code skysongclaims.admin}. */
public final class StaffCommand extends CommandGroup {
    public StaffCommand(StaffSupport support, Runnable reload) {
        super("sclaims", StaffSupport.ADMIN_PERMISSION, support.messenger());
        register(new CreateClaim(support));
        register(new DeleteClaim(support));
        register(new AddRegion(support));
        register(new RemoveRegion(support));
        register(new SetType(support));
        register(new SetFree(support));
        register(new SetMaxOwners(support));
        register(new RenameClaim(support));
        register(new ClaimInfo(support));
        register(new ListClaims(support));
        register(new ReloadCommand(support, reload));
    }
}
