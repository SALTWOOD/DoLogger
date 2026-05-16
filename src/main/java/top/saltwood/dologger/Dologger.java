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

@Mod(Dologger.MODID)
public class Dologger
{
    public static final String MODID = "dologger";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DatabaseManager databaseManager;

    public Dologger(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
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
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        if (databaseManager != null) {
            databaseManager.stop();
        }
    }

    public static DatabaseManager getDatabaseManager()
    {
        return databaseManager;
    }
}
