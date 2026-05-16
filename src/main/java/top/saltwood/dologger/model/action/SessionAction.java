package top.saltwood.dologger.model.action;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.Operation;
import top.saltwood.dologger.util.LanguageResolver;

public enum SessionAction implements IAction {
    JOIN(0, Operation.ADD),
    QUIT(1, Operation.REMOVE);

    private final int id;
    private final Operation operation;

    SessionAction(int id, Operation operation) {
        this.id = id;
        this.operation = operation;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Operation getOperation() {
        return operation;
    }

    public static @Nullable SessionAction fromId(int id) {
        for (SessionAction value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return null;
    }

    @Override
    public Component getPastTense() {
        return switch (this) {
            case JOIN -> LanguageResolver.component("dologger.action.join.past");
            case QUIT -> LanguageResolver.component("dologger.action.quit.past");
        };
    }
}
