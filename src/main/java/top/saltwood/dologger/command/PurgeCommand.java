package top.saltwood.dologger.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.saltwood.dologger.Config;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.HistoryPurger;
import top.saltwood.dologger.database.service.DatabaseStatus;
import top.saltwood.dologger.permission.Permissions;
import top.saltwood.dologger.util.LanguageResolver;

import java.sql.Connection;
import java.sql.SQLException;

public final class PurgeCommand {

    private PurgeCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("purge")
                .executes(context -> execute(context.getSource(), Config.purgeRetentionDays))
                .then(Commands.argument("days", IntegerArgumentType.integer(0, 36500))
                        .executes(context -> execute(context.getSource(), IntegerArgumentType.getInteger(context, "days"))));
    }

    private static int execute(CommandSourceStack source, int retentionDays) {
        if (!Permissions.canPurge(source)) {
            source.sendFailure(LanguageResolver.component("dologger.commands.error.permission"));
            return 0;
        }
        if (!DatabaseStatus.isAvailable()) {
            source.sendFailure(DatabaseStatus.unavailableComponent());
            return 0;
        }
        if (retentionDays <= 0) {
            source.sendFailure(LanguageResolver.component("dologger.commands.purge.disabled"));
            return 0;
        }

        try (Connection connection = Dologger.getDatabaseManager().getConnection()) {
            int deletedRows = HistoryPurger.purgeOlderThan(connection, retentionDays);
            source.sendSuccess(() -> LanguageResolver.component("dologger.commands.purge.success", retentionDays, deletedRows), true);
            return deletedRows;
        } catch (SQLException exception) {
            source.sendFailure(LanguageResolver.component("dologger.commands.purge.failed", exception.getMessage()));
            return 0;
        }
    }
}
