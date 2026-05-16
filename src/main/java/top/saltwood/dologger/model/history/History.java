package top.saltwood.dologger.model.history;

import net.minecraft.network.chat.Component;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.IAction;

public abstract class History implements IHistory {

    private final Time time;
    private final User user;
    private final BlockPosition position;
    private final IAction action;

    protected History(Time time, User user, BlockPosition position, IAction action) {
        this.time = time;
        this.user = user;
        this.position = position;
        this.action = action;
    }

    @Override
    public Time getTime() {
        return time;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public BlockPosition getPosition() {
        return position;
    }

    @Override
    public IAction getAction() {
        return action;
    }

    @Override
    public Component getComponentWithPos() {
        return getComponent().copy().append(" ").append(position.getComponent());
    }
}
