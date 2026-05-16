package top.saltwood.dologger.event;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.action.SessionAction;

public class PlayerEvents {

    private static final int MAX_MESSAGE_LENGTH = 256;

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        services.user().ensure(player.getUUID(), player.getGameProfile().getName());
        services.session().insert(player, SessionAction.JOIN);
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        services.session().insert(player, SessionAction.QUIT);
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        services.chat().insert(player, truncate(event.getMessage().getString()));
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        ParseResults<CommandSourceStack> parseResults = event.getParseResults();
        CommandSourceStack source = parseResults.getContext().getSource();
        if (!source.isPlayer()) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return;
        }

        services.command().insert(player, truncate(parseResults.getReader().getString()));
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH);
    }
}
