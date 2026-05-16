package top.saltwood.dologger.model.history;

import net.minecraft.core.component.DataComponentPatch;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.IAction;

public class ContainerHistory extends ItemHistory {

    public ContainerHistory(long time, String name, @Nullable String uuid, int x, int y, int z, String material, @Nullable DataComponentPatch data, int amount, int action) {
        super(time, name, uuid, x, y, z, material, data, amount, action);
    }

    public ContainerHistory(Time time, User user, BlockPosition position, SimpleItemStack itemStack, IAction action) {
        super(time, user, position, itemStack, action);
    }
}
