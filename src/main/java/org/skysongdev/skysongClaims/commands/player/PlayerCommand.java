package org.skysongdev.skysongClaims.commands.player;

import org.skysongdev.skysongClaims.commands.CommandGroup;
import org.skysongdev.skysongClaims.group.GroupService;

/** {@code /sclaim}: what players do with Claims. Owner commands check ownership themselves. */
public final class PlayerCommand extends CommandGroup {
    public PlayerCommand(PlayerSupport support, GroupService groups) {
        super("sclaim", PlayerSupport.USE_PERMISSION, support.messenger());
        register(new TakeSlot(support));
        register(new InviteCoOwner(support));
        register(new AnswerInvite(support, true));
        register(new AnswerInvite(support, false));
        register(new LeaveClaim(support));
        register(new GroupCommand(support, groups));
        register(new HereInfo(support));
        register(new OwnClaims(support));
    }
}
