package top.saltwood.dologger.model.history;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.CommandAction;

import java.util.UUID;

public class CommandHistory extends History {

    private final String command;

    public CommandHistory(long time, String name, @Nullable String uuid, int x, int y, int z, String command) {
        this(new Time(time), new User(name, uuid == null ? null : UUID.fromString(uuid)), new BlockPosition(x, y, z), command);
    }

    public CommandHistory(Time time, User user, BlockPosition position, String command) {
        super(time, user, position, CommandAction.COMMAND);
        this.command = command;
    }

    public String getCommand() {
        return command;
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
        return Component.literal(command).withStyle(ChatFormatting.GRAY);
    }
}
