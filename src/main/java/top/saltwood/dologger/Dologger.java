package top.saltwood.dologger;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DatabaseManager databaseManager;
    private static SqlQueue sqlQueue;
    private static Services services;
    private final BlockEvents blockEvents = new BlockEvents();
    private final ItemEvents itemEvents = new ItemEvents();

    public Dologger(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(blockEvents);
        NeoForge.EVENT_BUS.register(itemEvents);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("DoLogger common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("DoLogger server starting");
        databaseManager = new DatabaseManager();
        databaseManager.start();

        if (databaseManager.isAvailable()) {
            sqlQueue = new SqlQueue(databaseManager);
            sqlQueue.start();
            services = new Services();
            NeoForge.EVENT_BUS.register(new EntityEvents());
            NeoForge.EVENT_BUS.register(new PlayerEvents());
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
