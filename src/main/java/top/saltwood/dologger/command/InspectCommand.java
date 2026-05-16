package top.saltwood.dologger.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.DatabaseStatus;
import top.saltwood.dologger.permission.Permissions;
import top.saltwood.dologger.player.DoLoggerServerPlayer;

public final class InspectCommand {

    private InspectCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("inspect").executes(context -> execute(context.getSource()));
    }

    private static int execute(CommandSourceStack source) {
        if (!Permissions.canInspect(source)) {
            source.sendFailure(Component.translatable("dologger.commands.error.permission"));
            return 0;
        }
        if (!isDatabaseAvailable()) {
            source.sendFailure(DatabaseStatus.unavailableComponent());
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("dologger.commands.error.players_only"));
            return 0;
        }
        if (!(player instanceof DoLoggerServerPlayer doLoggerPlayer)) {
            source.sendFailure(Component.translatable("dologger.commands.error.player_state_unavailable"));
            return 0;
        }

        boolean inspecting = !doLoggerPlayer.dologger$isInspecting();
        doLoggerPlayer.dologger$setInspecting(inspecting);
        player.sendSystemMessage(Component.translatable(inspecting ? "dologger.commands.inspect.enabled" : "dologger.commands.inspect.disabled"));
        return 1;
    }

    private static boolean isDatabaseAvailable() {
        return Dologger.getServices() != null && Dologger.getServices().isDatabaseAvailable();
    }
}
