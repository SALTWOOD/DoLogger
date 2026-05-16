package top.saltwood.dologger.model.action;

import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.Operation;

public enum BlockAction implements IAction {
    BREAK_BLOCK(0, Operation.REMOVE),
    PLACE_BLOCK(1, Operation.ADD),
    INTERACT_BLOCK(2, Operation.NEUTRAL),
    KILL_ENTITY(3, Operation.REMOVE),
    INTERACT_ENTITY(4, Operation.NEUTRAL);

    private final int id;
    private final Operation operation;

    BlockAction(int id, Operation operation) {
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

    public static @Nullable BlockAction fromId(int id) {
        for (BlockAction value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return null;
    }
}
