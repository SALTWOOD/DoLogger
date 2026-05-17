package top.saltwood.dologger.model.action;

import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.Operation;

public enum CommandAction implements IAction {
    COMMAND(0, Operation.NEUTRAL);

    private final int id;
    private final Operation operation;

    CommandAction(int id, Operation operation) {
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

    public static @Nullable CommandAction fromId(int id) {
        return id == COMMAND.id ? COMMAND : null;
    }
}
