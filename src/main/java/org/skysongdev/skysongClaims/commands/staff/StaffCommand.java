package org.skysongdev.skysongClaims.commands.staff;

import org.skysongdev.skysongClaims.claim.ClaimCheck;
import org.skysongdev.skysongClaims.commands.CommandGroup;

import java.util.function.Supplier;

/** {@code /sclaims}: the staff commands. Every one needs {@code skysongclaims.admin}. */
public final class StaffCommand extends CommandGroup {
    /**
     * @param reload reloads every config file
     * @param check  runs {@code /sclaims check}
     */
    public StaffCommand(StaffSupport support, Runnable reload, Supplier<ClaimCheck.Report> check) {
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
        register(new CheckCommand(support, check));
        register(new ReloadCommand(support, reload));
    }
}
