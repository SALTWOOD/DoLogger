package top.saltwood.dologger.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.saltwood.dologger.Config;
import top.saltwood.dologger.permission.Permissions;
import top.saltwood.dologger.util.LanguageResolver;

public final class ReloadCommand {

    private ReloadCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("reload").executes(context -> execute(context.getSource()));
    }

    private static int execute(CommandSourceStack source) {
        if (!Permissions.canReload(source)) {
            source.sendFailure(LanguageResolver.component("dologger.commands.error.permission"));
            return 0;
        }

        Config.reload();
        LanguageResolver.reload();
        source.sendSuccess(() -> LanguageResolver.component("dologger.commands.reload.success", Config.language), true);
        return 1;
    }
}
