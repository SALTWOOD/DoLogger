package top.saltwood.dologger;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Dologger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Whether DoLogger logging is enabled")
            .define("enabled", true);

    public static final ModConfigSpec.ConfigValue<String> LANGUAGE = BUILDER
            .comment("Language file used by the server for DoLogger messages, from assets/dologger/lang/<language>.json")
            .define("language", "en_us");

    public static final ModConfigSpec.ConfigValue<String> HOST = BUILDER
            .comment("PostgreSQL host address")
            .define("host", "localhost");

    public static final ModConfigSpec.IntValue PORT = BUILDER
            .comment("PostgreSQL port")
            .defineInRange("port", 5432, 1, 65535);

    public static final ModConfigSpec.ConfigValue<String> DATABASE = BUILDER
            .comment("PostgreSQL database name")
            .define("database", "dologger");

    public static final ModConfigSpec.ConfigValue<String> USERNAME = BUILDER
            .comment("PostgreSQL username")
            .define("username", "postgres");

    public static final ModConfigSpec.ConfigValue<String> PASSWORD = BUILDER
            .comment("PostgreSQL password")
            .define("password", "");

    public static final ModConfigSpec.IntValue POOL_SIZE = BUILDER
            .comment("HikariCP maximum pool size")
            .defineInRange("poolSize", 3, 1, 100);

    public static final ModConfigSpec.IntValue CONNECTION_TIMEOUT = BUILDER
            .comment("HikariCP connection timeout in milliseconds")
            .defineInRange("connectionTimeout", 5000, 1000, 60000);

    public static final ModConfigSpec.IntValue VALIDATION_TIMEOUT = BUILDER
            .comment("HikariCP validation timeout in milliseconds")
            .defineInRange("validationTimeout", 3000, 1000, 30000);

    public static final ModConfigSpec.IntValue IDLE_TIMEOUT = BUILDER
            .comment("HikariCP idle timeout in milliseconds")
            .defineInRange("idleTimeout", 600000, 60000, 3600000);

    public static final ModConfigSpec.IntValue MAX_LIFETIME = BUILDER
            .comment("HikariCP max lifetime in milliseconds")
            .defineInRange("maxLifetime", 1800000, 60000, 7200000);

    public static final ModConfigSpec.IntValue QUEUE_FLUSH_TIMEOUT = BUILDER
            .comment("Queue flush timeout on server stop in milliseconds")
            .defineInRange("queueFlushTimeout", 10000, 1000, 60000);

    public static final ModConfigSpec.IntValue PAGE_SIZE = BUILDER
            .comment("Maximum number of entries per lookup page")
            .defineInRange("pageSize", 10, 1, 100);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static String language;
    public static String host;
    public static int port;
    public static String database;
    public static String username;
    public static String password;
    public static int poolSize;
    public static int connectionTimeout;
    public static int validationTimeout;
    public static int idleTimeout;
    public static int maxLifetime;
    public static int queueFlushTimeout;
    public static int pageSize;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        refresh();
    }

    public static void refresh() {
        enabled = ENABLED.get();
        language = LANGUAGE.get();
        host = HOST.get();
        port = PORT.get();
        database = DATABASE.get();
        username = USERNAME.get();
        password = PASSWORD.get();
        poolSize = POOL_SIZE.get();
        connectionTimeout = CONNECTION_TIMEOUT.get();
        validationTimeout = VALIDATION_TIMEOUT.get();
        idleTimeout = IDLE_TIMEOUT.get();
        maxLifetime = MAX_LIFETIME.get();
        queueFlushTimeout = QUEUE_FLUSH_TIMEOUT.get();
        pageSize = PAGE_SIZE.get();
    }

    public static void reload() {
        SPEC.afterReload();
        refresh();
    }
}
