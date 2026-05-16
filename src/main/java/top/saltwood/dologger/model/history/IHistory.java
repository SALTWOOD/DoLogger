package top.saltwood.dologger.model.history;

import net.minecraft.network.chat.Component;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.IAction;

public interface IHistory {

    Time getTime();

    User getUser();

    BlockPosition getPosition();

    IAction getAction();

    Component getComponent();

    Component getMaterialComponent();

    default Component getComponentWithPos() {
        return getComponent().copy().append(" ").append(getPosition().getComponent());
    }
}
