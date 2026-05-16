package top.saltwood.dologger.model.history;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.IAction;
import top.saltwood.dologger.model.action.ItemAction;

import java.util.UUID;

public class ItemHistory extends History {

    protected final SimpleItemStack itemStack;

    public ItemHistory(long time, String name, @Nullable String uuid, int x, int y, int z, String material, @Nullable DataComponentPatch data, int amount, int action) {
        this(new Time(time), new User(name, uuid == null ? null : UUID.fromString(uuid)), new BlockPosition(x, y, z), new SimpleItemStack(material, amount, data), ItemAction.fromId(action));
    }

    public ItemHistory(Time time, User user, BlockPosition position, SimpleItemStack itemStack, IAction action) {
        super(time, user, position, action);
        this.itemStack = itemStack;
    }

    public SimpleItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public Component getComponent() {
        MutableComponent component = getTime().getFormattedTimeAgo().append(" ")
                .append(getAction().getPrefix())
                .append(getUser().getNameComponent()).append(" ")
                .append(getAction().getPastTense()).append(" ");

        if (itemStack.getCount() > 1) {
            component.append(Component.literal(String.valueOf(itemStack.getCount()))).append(" ");
        }

        return component.append(getMaterialComponent());
    }

    @Override
    public Component getMaterialComponent() {
        MutableComponent name = itemStack.toItemStack().getHoverName().copy();
        return name.withStyle(name.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(itemStack.toItemStack().copyWithCount(1)))));
    }
}
