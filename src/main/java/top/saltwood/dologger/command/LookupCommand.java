package top.saltwood.dologger.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.command.filter.FilterList;
import top.saltwood.dologger.command.filter.FilterParseException;
import top.saltwood.dologger.command.filter.FilterParser;
import top.saltwood.dologger.database.service.DatabaseStatus;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.history.IHistory;
import top.saltwood.dologger.permission.Permissions;
import top.saltwood.dologger.player.DoLoggerServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LookupCommand {

    static final int PAGE_SIZE = 10;

    private LookupCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("lookup")
                .executes(context -> execute(context.getSource(), ""))
                .then(Commands.argument("filters", StringArgumentType.greedyString())
                        .executes(context -> execute(context.getSource(), StringArgumentType.getString(context, "filters"))));
    }

    private static int execute(CommandSourceStack source, String filters) {
        if (!Permissions.canLookup(source)) {
            source.sendFailure(Component.translatable("dologger.commands.error.permission"));
            return 0;
        }
        Services services = Dologger.getServices();
        if (services == null || !services.isDatabaseAvailable()) {
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

        FilterList filterList;
        try {
            filterList = FilterParser.parse(filters, player.blockPosition());
        } catch (FilterParseException exception) {
            source.sendFailure(Component.translatable("dologger.commands.lookup.parse_error", exception.getMessage()));
            return 0;
        }

        List<IHistory> history = new ArrayList<>();
        List<Object> repositoryParams = filterList.toRepositoryParams();
        history.addAll(services.block().getFilteredBlockHistory(player.level(), repositoryParams));
        history.addAll(services.container().getFilteredContainerHistory(player.level(), repositoryParams));
        history.addAll(services.item().getFilteredItemHistory(player.level(), repositoryParams));
        history.addAll(services.session().getFilteredSessionHistory(player.level(), filterList.toSessionRepositoryParams()));
        history.sort(Comparator.comparingLong((IHistory entry) -> entry.getTime().time()).reversed());

        if (history.isEmpty()) {
            doLoggerPlayer.dologger$setPages(List.of());
            source.sendSuccess(() -> Component.translatable("dologger.commands.lookup.no_results"), false);
            return 1;
        }

        List<List<IHistory>> pages = paginate(history);
        doLoggerPlayer.dologger$setPages(pages);
        PageCommand.displayPage(source, pages, 1);
        return history.size();
    }

    private static List<List<IHistory>> paginate(List<IHistory> history) {
        List<List<IHistory>> pages = new ArrayList<>();
        for (int index = 0; index < history.size(); index += PAGE_SIZE) {
            pages.add(List.copyOf(history.subList(index, Math.min(index + PAGE_SIZE, history.size()))));
        }
        return List.copyOf(pages);
    }
}
