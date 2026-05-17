package top.saltwood.dologger.model.history;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.ChatAction;

import java.util.UUID;

public class ChatHistory extends History {

    private final String message;

    public ChatHistory(long time, String name, @Nullable String uuid, int x, int y, int z, String message) {
        this(new Time(time), new User(name, uuid == null ? null : UUID.fromString(uuid)), new BlockPosition(x, y, z), message);
    }

    public ChatHistory(Time time, User user, BlockPosition position, String message) {
        super(time, user, position, ChatAction.CHAT);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public Component getComponent() {
        return getTime().getFormattedTimeAgo().append(" ")
                .append(getAction().getPrefix())
                .append(getUser().getNameComponent()).append(" ")
                .append(getAction().getPastTense()).append(" ")
                .append(getMaterialComponent());
    }

    @Override
    public Component getMaterialComponent() {
        return Component.literal(message).withStyle(ChatFormatting.GRAY);
    }
}
