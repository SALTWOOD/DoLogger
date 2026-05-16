package top.saltwood.dologger.database.service;

import net.minecraft.network.chat.Component;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.DatabaseManager;

public final class DatabaseStatus {

    private static final String UNAVAILABLE_TRANSLATION_KEY = "dologger.database.unavailable";
    private static final String UNAVAILABLE_TEXT = "DoLogger database is unavailable; logging and lookup commands are disabled.";

    private DatabaseStatus() {
    }

    public static boolean isAvailable() {
        DatabaseManager databaseManager = Dologger.getDatabaseManager();
        return databaseManager != null && databaseManager.isAvailable();
    }

    public static Component unavailableComponent() {
        return Component.translatableWithFallback(UNAVAILABLE_TRANSLATION_KEY, UNAVAILABLE_TEXT);
    }

    public static String unavailableMessage() {
        return UNAVAILABLE_TEXT;
    }
}
