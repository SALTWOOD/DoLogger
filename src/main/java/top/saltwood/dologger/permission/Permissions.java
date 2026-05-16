package top.saltwood.dologger.permission;

import net.minecraft.commands.CommandSourceStack;

public final class Permissions {

    public static final String INSPECT = "dologger.inspect";
    public static final String LOOKUP = "dologger.lookup";
    public static final String PAGE = "dologger.page";
    public static final String RELOAD = "dologger.reload";
    public static final String PURGE = "dologger.purge";
    public static final String REVERT = "dologger.revert";
    public static final String RESTORE = "dologger.restore";
    public static final int DEFAULT_OP_LEVEL = 2;

    private Permissions() {
    }

    public static boolean canInspect(CommandSourceStack source) {
        return check(source, INSPECT);
    }

    public static boolean canLookup(CommandSourceStack source) {
        return check(source, LOOKUP);
    }

    public static boolean canPage(CommandSourceStack source) {
        return check(source, PAGE);
    }

    public static boolean canReload(CommandSourceStack source) {
        return check(source, RELOAD);
    }

    public static boolean canPurge(CommandSourceStack source) {
        return check(source, PURGE);
    }

    public static boolean canRevert(CommandSourceStack source) {
        return check(source, REVERT, DEFAULT_OP_LEVEL);
    }

    public static boolean canRestore(CommandSourceStack source) {
        return check(source, RESTORE, DEFAULT_OP_LEVEL);
    }

    public static boolean check(CommandSourceStack source, String permissionNode) {
        return check(source, permissionNode, DEFAULT_OP_LEVEL);
    }

    public static boolean check(CommandSourceStack source, String permissionNode, int fallbackLevel) {
        return source != null && source.hasPermission(fallbackLevel);
    }
}
