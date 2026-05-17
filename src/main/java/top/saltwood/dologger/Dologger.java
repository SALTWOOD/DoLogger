package top.saltwood.dologger;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import top.saltwood.dologger.command.DoLoggerCommand;
import top.saltwood.dologger.database.DatabaseManager;
import top.saltwood.dologger.database.SqlQueue;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.event.BlockEvents;
import top.saltwood.dologger.event.EntityEvents;
import top.saltwood.dologger.event.PlayerEvents;
import top.saltwood.dologger.event.item.ItemEvents;

@Mod(Dologger.MODID)
public class Dologger
{
    public static final String MODID = "dologger";
    private static final long RECONNECT_INTERVAL_MS = 30_000L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DatabaseManager databaseManager;
    private static SqlQueue sqlQueue;
    private static Services services;
    private static long nextReconnectAttemptMillis;
    private final BlockEvents blockEvents = new BlockEvents();
    private final EntityEvents entityEvents = new EntityEvents();
    private final ItemEvents itemEvents = new ItemEvents();
    private final PlayerEvents playerEvents = new PlayerEvents();

    public Dologger(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(blockEvents);
        NeoForge.EVENT_BUS.register(entityEvents);
        NeoForge.EVENT_BUS.register(itemEvents);
        NeoForge.EVENT_BUS.register(playerEvents);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("DoLogger common setup");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DoLoggerCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("DoLogger server starting");
        databaseManager = new DatabaseManager();
        databaseManager.start();

        initializeServicesIfAvailable();
        nextReconnectAttemptMillis = System.currentTimeMillis() + RECONNECT_INTERVAL_MS;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (services != null || databaseManager == null || databaseManager.isAvailable() || !Config.enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextReconnectAttemptMillis) {
            return;
        }

        nextReconnectAttemptMillis = now + RECONNECT_INTERVAL_MS;
        if (databaseManager.tryReconnect()) {
            initializeServicesIfAvailable();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        if (sqlQueue != null) {
            sqlQueue.shutdown();
            sqlQueue = null;
        }

        services = null;

        if (databaseManager != null) {
            databaseManager.stop();
            databaseManager = null;
        }
    }

    private static void initializeServicesIfAvailable() {
        if (databaseManager == null || !databaseManager.isAvailable() || sqlQueue != null || services != null) {
            return;
        }

        sqlQueue = new SqlQueue(databaseManager);
        sqlQueue.start();
        services = new Services();
        LOGGER.info("DoLogger: Database services initialized");
    }

    public static DatabaseManager getDatabaseManager()
    {
        return databaseManager;
    }

    public static SqlQueue getSqlQueue()
    {
        return sqlQueue;
    }

    public static Services getServices()
    {
        return services;
    }
}
