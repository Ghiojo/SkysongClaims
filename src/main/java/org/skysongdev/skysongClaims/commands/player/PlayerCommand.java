package org.skysongdev.skysongClaims.commands.player;

import org.skysongdev.skysongClaims.commands.CommandGroup;

/** {@code /sclaim}: what players do with Claims. Owner commands check ownership themselves. */
public final class PlayerCommand extends CommandGroup {
    public PlayerCommand(PlayerSupport support) {
        super("sclaim", PlayerSupport.USE_PERMISSION, support.messenger());
        register(new TakeSlot(support));
        register(new InviteCoOwner(support));
        register(new AnswerInvite(support, true));
        register(new AnswerInvite(support, false));
        register(new LeaveClaim(support));
        register(new HereInfo(support));
        register(new OwnClaims(support));
    }
}
