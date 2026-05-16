package top.saltwood.dologger.model.action;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import top.saltwood.dologger.model.Operation;
import top.saltwood.dologger.util.LanguageResolver;

public interface IAction {

    String name();

    int getId();

    Operation getOperation();

    default Component getPrefix() {
        return switch (getOperation()) {
            case ADD -> Component.literal("+ ").withStyle(ChatFormatting.GREEN);
            case REMOVE -> Component.literal("- ").withStyle(ChatFormatting.RED);
            case NEUTRAL -> Component.literal("~ ").withStyle(ChatFormatting.GRAY);
        };
    }

    default Component getPastTense() {
        return LanguageResolver.component("dologger.action." + name().toLowerCase() + ".past");
    }
}
