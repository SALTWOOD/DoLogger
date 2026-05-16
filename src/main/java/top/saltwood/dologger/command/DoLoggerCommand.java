package top.saltwood.dologger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class DoLoggerCommand {

    private DoLoggerCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("dologger"));
        dispatcher.register(root("gl"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .then(InspectCommand.register())
                .then(LookupCommand.register())
                .then(PageCommand.register());
    }
}
