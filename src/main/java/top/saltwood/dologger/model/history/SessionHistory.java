package top.saltwood.dologger.model.history;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.IAction;
import top.saltwood.dologger.model.action.SessionAction;

import java.util.UUID;

public class SessionHistory extends History {

    public SessionHistory(long time, String name, @Nullable String uuid, int x, int y, int z, int sessionAction) {
        this(new Time(time), new User(name, uuid == null ? null : UUID.fromString(uuid)), new BlockPosition(x, y, z), SessionAction.fromId(sessionAction));
    }

    public SessionHistory(Time time, User user, BlockPosition position, IAction action) {
        super(time, user, position, action);
    }

    @Override
    public Component getComponent() {
        return getTime().getFormattedTimeAgo().append(" ")
                .append(getAction().getPrefix())
                .append(getUser().getNameComponent()).append(" ")
                .append(getAction().getPastTense());
    }

    @Override
    public Component getMaterialComponent() {
        return Component.empty();
    }
}
