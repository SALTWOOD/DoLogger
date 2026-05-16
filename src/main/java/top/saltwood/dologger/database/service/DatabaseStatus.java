package top.saltwood.dologger.database.service;

import net.minecraft.network.chat.Component;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.DatabaseManager;
import top.saltwood.dologger.util.LanguageResolver;

public final class DatabaseStatus {

    private static final String UNAVAILABLE_TRANSLATION_KEY = "dologger.database.unavailable";

    private DatabaseStatus() {
    }

    public static boolean isAvailable() {
        DatabaseManager databaseManager = Dologger.getDatabaseManager();
        return databaseManager != null && databaseManager.isAvailable();
    }

    public static Component unavailableComponent() {
        return LanguageResolver.component(UNAVAILABLE_TRANSLATION_KEY);
    }

    public static String unavailableMessage() {
        return LanguageResolver.resolve(UNAVAILABLE_TRANSLATION_KEY);
    }
}
