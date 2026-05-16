package top.saltwood.dologger.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.saltwood.dologger.model.history.IHistory;
import top.saltwood.dologger.permission.Permissions;
import top.saltwood.dologger.player.DoLoggerServerPlayer;

import java.util.List;

public final class PageCommand {

    private PageCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("page")
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> execute(context.getSource(), IntegerArgumentType.getInteger(context, "page"))));
    }

    private static int execute(CommandSourceStack source, int pageNumber) {
        if (!Permissions.canPage(source)) {
            source.sendFailure(Component.translatable("dologger.commands.error.permission"));
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

        List<List<IHistory>> pages = doLoggerPlayer.dologger$getPages();
        if (pages == null || pages.isEmpty()) {
            source.sendFailure(Component.translatable("dologger.commands.page.no_results"));
            return 0;
        }
        if (pageNumber > pages.size()) {
            source.sendFailure(Component.translatable("dologger.commands.page.out_of_bounds", pageNumber, pages.size()));
            return 0;
        }

        displayPage(source, pages, pageNumber);
        return 1;
    }

    static void displayPage(CommandSourceStack source, List<List<IHistory>> pages, int pageNumber) {
        source.sendSuccess(() -> Component.translatable("dologger.commands.lookup.header", pageNumber, pages.size()), false);
        for (IHistory entry : pages.get(pageNumber - 1)) {
            source.sendSuccess(entry::getComponent, false);
        }
        source.sendSuccess(() -> Component.translatable("dologger.commands.lookup.footer", pageNumber, pages.size()), false);
    }
}
