package top.saltwood.dologger.permission;

import net.minecraft.commands.CommandSourceStack;

public final class Permissions {

    public static final String INSPECT = "grieflogger.inspect";
    public static final String LOOKUP = "grieflogger.lookup";
    public static final String PAGE = "grieflogger.page";
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

    public static boolean check(CommandSourceStack source, String permissionNode) {
        return check(source, permissionNode, DEFAULT_OP_LEVEL);
    }

    public static boolean check(CommandSourceStack source, String permissionNode, int fallbackLevel) {
        return source != null && source.hasPermission(fallbackLevel);
    }
}
